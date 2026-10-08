import { BrandMark } from "@fixna/brand";

export type BrandMarkIconProps = {
  className?: string;
  width?: number;
  height?: number;
};

/** Header/sidebar logo mark — shared ecosystem artwork. */
export function BrandMarkIcon({
  className,
  width = 42,
  height = 44,
}: BrandMarkIconProps) {
  return <BrandMark className={className} width={width} height={height} />;
}
