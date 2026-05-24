"""Generate photo-realistic glass sphere and metal pipe textures for breathing canvas."""
from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res" / "drawable-nodpi"


def clamp(v: float, lo: float = 0.0, hi: float = 1.0) -> float:
    return max(lo, min(hi, v))


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def write_png(path: Path, width: int, height: int, rgba: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)

    def chunk(tag: bytes, data: bytes) -> bytes:
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    raw = b"".join(
        b"\x00" + rgba[y * width * 4 : (y + 1) * width * 4]
        for y in range(height)
    )
    compressed = zlib.compress(raw, 9)
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", compressed) + chunk(b"IEND", b"")
    path.write_bytes(png)


def generate_glass_sphere(size: int = 512) -> bytes:
    cx = cy = (size - 1) / 2.0
    radius = size * 0.46
    pixels = bytearray(size * size * 4)
    for y in range(size):
        for x in range(size):
            dx = x - cx
            dy = y - cy
            dist = math.sqrt(dx * dx + dy * dy)
            nd = dist / radius
            i = (y * size + x) * 4
            if nd > 1.05:
                pixels[i : i + 4] = (0, 0, 0, 0)
                continue

            # Fresnel rim + inner cavity shading
            fresnel = clamp((nd - 0.72) / 0.28) ** 1.6
            inner = clamp(1.0 - nd * 0.95) ** 2.2 * 0.35

            # Primary specular crescent (upper-left)
            hx, hy = -0.38, -0.42
            spec = math.exp(-(((dx / radius - hx) ** 2) / 0.018 + ((dy / radius - hy) ** 2) / 0.055)) * 0.95
            spec2 = math.exp(-(((dx / radius + 0.15) ** 2) / 0.006 + ((dy / radius + 0.08) ** 2) / 0.012)) * 0.35

            # Bottom caustic glow
            caustic = math.exp(-(((dx / radius) ** 2) / 0.25 + ((dy / radius - 0.55) ** 2) / 0.08)) * 0.22

            # Subtle chromatic edge tint
            edge_r = fresnel * 0.75
            edge_g = fresnel * 0.82
            edge_b = fresnel * 0.95

            alpha = clamp(fresnel * 0.55 + inner * 0.25 + spec * 0.45 + caustic * 0.2, 0.0, 0.92)
            r = clamp(spec + spec2 + edge_r * 0.4 + caustic * 0.15, 0.0, 1.0)
            g = clamp(spec * 0.98 + spec2 * 0.95 + edge_g * 0.42 + caustic * 0.18, 0.0, 1.0)
            b = clamp(spec * 0.95 + spec2 * 0.9 + edge_b * 0.5 + caustic * 0.25, 0.0, 1.0)

            if nd > 1.0:
                alpha *= clamp(1.0 - (nd - 1.0) / 0.05, 0.0, 1.0)

            pixels[i : i + 4] = (
                int(r * 255),
                int(g * 255),
                int(b * 255),
                int(alpha * 255),
            )
    return bytes(pixels)


def generate_pipe_texture(width: int = 128, height: int = 512) -> bytes:
    pixels = bytearray(width * height * 4)
    for y in range(height):
        flange = 0.0
        t = y / max(height - 1, 1)
        if t < 0.06 or t > 0.94:
            flange = 1.0 - min(t, 1.0 - t) / 0.06
        for x in range(width):
            u = x / max(width - 1, 1)
            # Cylindrical shading: dark edges, bright center-left
            cylinder = 1.0 - abs(u - 0.5) * 2.0
            cylinder = cylinder**1.35
            spec = math.exp(-((u - 0.34) ** 2) / 0.0045) * 0.85
            groove = 0.5 + 0.5 * math.sin(y * 0.22 + math.sin(y * 0.04) * 2.0)
            scratch = 0.92 + 0.08 * math.sin(y * 0.9 + u * 14.0)

            base = lerp(0.18, 0.42, cylinder) * scratch
            base += groove * 0.04
            base += spec
            base += flange * 0.18

            r = clamp(base * 0.95 + spec * 0.08, 0.0, 1.0)
            g = clamp(base * 0.98 + spec * 0.1, 0.0, 1.0)
            b = clamp(base * 1.05 + spec * 0.12, 0.0, 1.0)
            alpha = clamp(0.55 + cylinder * 0.35 + spec * 0.25 + flange * 0.2, 0.0, 0.98)

            i = (y * width + x) * 4
            pixels[i : i + 4] = (int(r * 255), int(g * 255), int(b * 255), int(alpha * 255))
    return bytes(pixels)


def main() -> None:
    write_png(OUT / "breath_glass_sphere.png", 512, 512, generate_glass_sphere())
    write_png(OUT / "breath_pipe_texture.png", 128, 512, generate_pipe_texture())
    print(f"Wrote assets to {OUT}")


if __name__ == "__main__":
    main()
