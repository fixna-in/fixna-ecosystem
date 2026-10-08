import { BrandMark } from "@fixna/brand";

export type BrandMarkIconProps = {
  className?: string;
  width?: number;
  height?: number;
};

/** Shared Fixna brand mark for all products. */
export function BrandMarkIcon({ className, width = 42, height = 44 }: BrandMarkIconProps) {
  return <BrandMark className={className} width={width} height={height} />;
}
