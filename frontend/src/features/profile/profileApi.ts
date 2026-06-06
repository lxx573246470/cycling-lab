import { z } from "zod";
import { api } from "@/lib/api";

export const bikeSchema = z.object({
  name: z.string().min(1).max(64),
  type: z.enum(["road", "gravel", "tt", "mtb", "indoor"]),
  mileageKm: z.number().nonnegative().optional().nullable(),
  notes: z.string().max(500).optional().nullable(),
});

export const riderProfileSchema = z.object({
  displayName: z.string().min(1).max(64),
  heightCm: z.number().int().min(100).max(230),
  weightKg: z.number().min(30).max(200),
  maxHr: z.number().int().min(100).max(230),
  restingHr: z.number().int().min(30).max(120).optional().nullable(),
  thresholdHr: z.number().int().min(100).max(230).optional().nullable(),
  ftp: z.number().int().min(50).max(600),
  cadenceLow: z.number().int().min(40).max(130),
  cadenceHigh: z.number().int().min(40).max(130),
  bikes: z.array(bikeSchema).default([]),
  powerMeter: z.string().max(128).optional().nullable(),
  hrStrap: z.string().max(128).optional().nullable(),
  headUnit: z.string().max(128).optional().nullable(),
  goals: z.record(z.string(), z.string()).default({}),
  preferences: z.record(z.string(), z.unknown()).default({}),
  isPublic: z.boolean().default(false),
}).refine((v) => v.cadenceLow < v.cadenceHigh, {
  message: "cadenceLow must be < cadenceHigh",
  path: ["cadenceHigh"],
}).refine((v) => !v.thresholdHr || v.thresholdHr < v.maxHr, {
  message: "thresholdHr must be < maxHr",
  path: ["thresholdHr"],
});

export type RiderProfile = z.infer<typeof riderProfileSchema>;

export interface RiderProfileDto {
  userId: string;
  displayName: string;
  heightCm: number;
  weightKg: number;
  maxHr: number;
  restingHr: number | null;
  thresholdHr: number | null;
  ftp: number;
  cadenceLow: number;
  cadenceHigh: number;
  bikes: RiderProfile["bikes"];
  powerMeter: string | null;
  hrStrap: string | null;
  headUnit: string | null;
  goals: Record<string, string>;
  preferences: Record<string, unknown>;
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface DerivedZones {
  hrZones: { zone: number; name: string; low: number; high: number; bpmLow: number; bpmHigh: number }[];
  powerZones: { zone: number; name: string; low: number; high: number; wattsLow: number; wattsHigh: number }[];
  cadenceRange: { low: number; high: number };
  ftp: number;
  maxHr: number;
  thresholdHr: number;
  computedAt: string;
}

export const profileApi = {
  get: () => api<RiderProfileDto | null>("/profile", { method: "GET" }),
  put: (values: RiderProfile) =>
    api<RiderProfileDto>("/profile", { method: "PUT", body: values }),
  patch: (values: Partial<RiderProfile>) =>
    api<RiderProfileDto>("/profile", { method: "PATCH", body: values }),
  derivedZones: () => api<DerivedZones>("/profile/derived-zones", { method: "GET" }),
  export: () => api<Record<string, unknown>>("/profile/export", { method: "GET" }),
};
