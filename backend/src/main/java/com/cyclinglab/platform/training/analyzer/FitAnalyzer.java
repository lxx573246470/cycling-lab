package com.cyclinglab.platform.training.analyzer;

import com.cyclinglab.platform.plan.IsoWeek;
import com.garmin.fit.DateTime;
import com.garmin.fit.Decode;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgListener;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure function that turns a FIT file on disk into an {@link AnalysisResult}.
 * Uses the official Garmin FIT SDK so we never need a Python runtime.
 *
 * <p>Status, on-disk layout, and persistence are the caller's concern; this
 * class does not touch the database.
 */
public final class FitAnalyzer {

    private FitAnalyzer() {}

    public static AnalysisResult analyze(Path fitFile) throws IOException {
        if (!Files.isRegularFile(fitFile)) {
            throw new IOException("FIT file not found: " + fitFile);
        }
        CollectingListener listener = new CollectingListener();
        Decode decode = new Decode();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(fitFile))) {
            decode.read(in, (MesgListener) listener);
        } catch (Throwable t) {
            throw new IOException("Failed to parse FIT file: " + t.getMessage(), t);
        }
        return compute(listener);
    }

    static AnalysisResult compute(CollectingListener listener) {
        FileIdMesg fileId = listener.firstFileId();
        SessionMesg session = listener.firstSession();

        Instant startedAt = resolveStart(fileId, session);
        String device = fileId == null ? null : fileId.getProductName();
        String sport = resolveSport(fileId, session);

        // Aggregate metrics
        int sampleCount = 0;
        int durationSec = 0;
        Double distanceM = null;
        Double energyKj = null;
        int avgHr = 0, maxHr = 0, avgPower = 0, maxPower = 0;
        int avgCadence = 0, maxCadence = 0;
        int npower = 0;
        Double drift = null;
        Integer isoYear = null, isoWeek = null;

        if (session != null) {
            Float t = session.getTotalTimerTime();
            if (t == null) t = session.getTotalElapsedTime();
            if (t != null) durationSec = Math.round(t);

            Float dist = session.getTotalDistance();
            if (dist != null) distanceM = (double) dist;

            Integer cal = session.getTotalCalories();
            if (cal != null) energyKj = cal * 4.184;

            Short s;
            if ((s = session.getAvgHeartRate()) != null) avgHr = s.intValue();
            if ((s = session.getMaxHeartRate()) != null) maxHr = s.intValue();
            Integer p;
            if ((p = session.getAvgPower()) != null) avgPower = p;
            if ((p = session.getMaxPower()) != null) maxPower = p;
            if ((s = session.getAvgCadence()) != null) avgCadence = s.intValue();
            if ((s = session.getMaxCadence()) != null) maxCadence = s.intValue();
            if ((p = session.getNormalizedPower()) != null) npower = p;
        }

        // Build samples from the per-second records
        List<Sample> samples = new ArrayList<>();
        long baseMs = -1L;
        for (RecordMesg r : listener.recordMesgs()) {
            DateTime t = r.getTimestamp();
            if (t == null) continue;
            Long ms = t.getTimestamp();
            if (ms == null) continue;
            if (baseMs < 0) baseMs = ms;
            int offset = (int) ((ms - baseMs) / 1000L);
            Integer hr = r.getHeartRate() == null ? null : r.getHeartRate().intValue();
            Integer power = r.getPower();
            Integer cadence = r.getCadence() == null ? null : r.getCadence().intValue();
            Float speed = r.getSpeed();
            Float altitude = r.getEnhancedAltitude();
            if (altitude == null) altitude = r.getAltitude();
            Integer lat = r.getPositionLat();
            Integer lon = r.getPositionLong();
            samples.add(new Sample(
                offset,
                hr, power, cadence,
                speed == null ? null : (double) speed,
                altitude == null ? null : (double) altitude,
                lat == null ? null : semicirclesToDegrees(lat),
                lon == null ? null : semicirclesToDegrees(lon)
            ));
            sampleCount++;
        }

        // Recompute max HR / power / cadence from records when session didn't
        // expose them (some indoor trainers don't write session summary).
        if (!samples.isEmpty()) {
            int curMaxHr = 0, curMaxPower = 0, curMaxCad = 0;
            long sumHr = 0, cntHr = 0, sumPower = 0, cntPower = 0, sumCad = 0, cntCad = 0;
            for (Sample s : samples) {
                if (s.hr() != null) { if (s.hr() > curMaxHr) curMaxHr = s.hr(); sumHr += s.hr(); cntHr++; }
                if (s.power() != null) { if (s.power() > curMaxPower) curMaxPower = s.power(); sumPower += s.power(); cntPower++; }
                if (s.cadence() != null) { if (s.cadence() > curMaxCad) curMaxCad = s.cadence(); sumCad += s.cadence(); cntCad++; }
            }
            if (maxHr == 0) maxHr = curMaxHr;
            if (maxPower == 0) maxPower = curMaxPower;
            if (maxCadence == 0) maxCadence = curMaxCad;
            if (avgHr == 0 && cntHr > 0) avgHr = (int) Math.round(sumHr / (double) cntHr);
            if (avgPower == 0 && cntPower > 0) avgPower = (int) Math.round(sumPower / (double) cntPower);
            if (avgCadence == 0 && cntCad > 0) avgCadence = (int) Math.round(sumCad / (double) cntCad);
            if (durationSec == 0) {
                durationSec = samples.get(samples.size() - 1).tOffsetSec();
            }
        }

        // Normalized power: prefer session field; otherwise compute from samples
        if (npower <= 0) {
            int np = NormalizedPowerCalculator.compute(samples);
            if (np > 0) npower = np;
        }

        // HR drift (decoupling) over the second half vs the first half
        drift = HrDriftCalculator.compute(samples);

        // Derived metrics
        IntensityFactorCalculator.IfResult ifr = IntensityFactorCalculator.compute(npower);
        Double intensityFactor = ifr.intensityFactor();
        Double tss = IntensityFactorCalculator.tss(durationSec, npower, null);

        // Distributions + segments
        String hrZones = ZoneDistributions.hrJson(samples, avgHr);
        String powerZones = ZoneDistributions.powerJson(samples);
        String cadenceZones = ZoneDistributions.cadenceJson(samples);
        List<TenMinSegment> tenMin = TenMinSegmentCalculator.compute(samples);
        List<BestRolling> best = BestRollingCalculator.compute(samples);

        if (startedAt != null) {
            int[] yw = IsoWeek.of(startedAt.atZone(java.time.ZoneOffset.UTC).toLocalDate());
            isoYear = yw[0];
            isoWeek = yw[1];
        }

        return new AnalysisResult(
            startedAt,
            sport,
            device,
            durationSec,
            distanceM,
            energyKj,
            avgHr, maxHr,
            avgPower, maxPower,
            npower,
            intensityFactor,
            tss,
            avgCadence, maxCadence,
            drift,
            hrZones,
            powerZones,
            cadenceZones,
            tenMin,
            best,
            samples,
            sampleCount,
            isoYear,
            isoWeek
        );
    }

    private static Integer toInt(Short s) {
        return s == null ? null : s.intValue();
    }

    private static Instant resolveStart(FileIdMesg fileId, SessionMesg session) {
        if (session != null) {
            DateTime t = session.getStartTime();
            Long ms = t == null ? null : t.getTimestamp();
            if (ms != null) return t.getInstant();
        }
        if (fileId != null) {
            DateTime t = fileId.getTimeCreated();
            Long ms = t == null ? null : t.getTimestamp();
            if (ms != null) return t.getInstant();
        }
        return null;
    }

    private static String resolveSport(FileIdMesg fileId, SessionMesg session) {
        if (session != null) {
            Sport s = session.getSport();
            if (s != null) {
                if ("CYCLING".equals(s.name()) || "E_BIKING".equals(s.name())) {
                    return "cycling";
                }
                return s.name().toLowerCase();
            }
        }
        if (fileId != null && fileId.getType() != null) {
            return fileId.getType().toString().toLowerCase();
        }
        return "cycling";
    }private static Double semicirclesToDegrees(int semi) {
        return semi * (180.0 / Math.pow(2, 31));
    }

    /**
     * Stand-alone {@link MesgListener} that buffers all messages from a FIT
     * decode into typed lists. We can't use the SDK's {@code FitMessages}
     * because its fields are package-private.
     */
    static final class CollectingListener implements MesgListener {
        private final List<FileIdMesg> fileIds = new ArrayList<>();
        private final List<SessionMesg> sessions = new ArrayList<>();
        private final List<RecordMesg> records = new ArrayList<>();

        FileIdMesg firstFileId() {
            return fileIds.isEmpty() ? null : fileIds.get(0);
        }

        SessionMesg firstSession() {
            return sessions.isEmpty() ? null : sessions.get(0);
        }

        List<RecordMesg> recordMesgs() {
            return records;
        }

        @Override
        public void onMesg(Mesg mesg) {
            switch (mesg.getNum()) {
                case com.garmin.fit.MesgNum.FILE_ID:
                    fileIds.add(new FileIdMesg(mesg));
                    break;
                case com.garmin.fit.MesgNum.SESSION:
                    sessions.add(new SessionMesg(mesg));
                    break;
                case com.garmin.fit.MesgNum.RECORD:
                    records.add(new RecordMesg(mesg));
                    break;
                default:
                    // ignore
            }
        }
    }
}
