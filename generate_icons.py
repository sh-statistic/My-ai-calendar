#!/usr/bin/env python3
import os
import math
from PIL import Image, ImageDraw

def create_app_icon(size, is_round=False):
    # Base image with warm amber / solar gradient background
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    margin = size * 0.05
    # Background rectangle with rounded corners or circle
    bg_color = (217, 119, 6, 255) # Warm Solar Amber #D97706
    bg_dark = (180, 83, 9, 255)   # #B45309

    if is_round:
        draw.ellipse([0, 0, size - 1, size - 1], fill=bg_color)
    else:
        corner_rad = size * 0.22
        draw.rounded_rectangle([margin, margin, size - 1 - margin, size - 1 - margin], radius=corner_rad, fill=bg_color)

    # Inner decorative solar disc & calendar grid
    center = size / 2.0
    sun_rad = size * 0.26
    draw.ellipse([center - sun_rad, center - sun_rad - size * 0.06, center + sun_rad, center + sun_rad - size * 0.06], fill=(254, 243, 199, 255)) # #FEF3C7

    # Draw Calendar Grid Plate at bottom
    cal_top = center - size * 0.02
    cal_bottom = size * 0.82
    cal_left = size * 0.24
    cal_right = size * 0.76
    cal_rad = size * 0.08
    draw.rounded_rectangle([cal_left, cal_top, cal_right, cal_bottom], radius=cal_rad, fill=(255, 255, 255, 245))

    # Calendar header bar (Amber)
    draw.rounded_rectangle([cal_left, cal_top, cal_right, cal_top + (cal_bottom - cal_top) * 0.32], radius=cal_rad, fill=(180, 83, 9, 255))

    # Calendar dots / dates
    grid_y = cal_top + (cal_bottom - cal_top) * 0.55
    for row in range(2):
        for col in range(4):
            dx = cal_left + size * 0.10 + col * (size * 0.11)
            dy = grid_y + row * (size * 0.11)
            draw.ellipse([dx - size * 0.02, dy - size * 0.02, dx + size * 0.02, dy + size * 0.02], fill=(217, 119, 6, 220))

    return img

densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

base_res = "/app/src/main/res"

for folder, size in densities.items():
    dir_path = os.path.join(base_res, folder)
    os.makedirs(dir_path, exist_ok=True)

    # Remove existing .webp
    webp_std = os.path.join(dir_path, "ic_launcher.webp")
    webp_round = os.path.join(dir_path, "ic_launcher_round.webp")
    if os.path.exists(webp_std): os.remove(webp_std)
    if os.path.exists(webp_round): os.remove(webp_round)

    # Create PNGs
    icon_square = create_app_icon(size, is_round=False)
    icon_square.save(os.path.join(dir_path, "ic_launcher.png"), "PNG")

    icon_round = create_app_icon(size, is_round=True)
    icon_round.save(os.path.join(dir_path, "ic_launcher_round.png"), "PNG")

# Also save a drawable version
drawable_path = os.path.join(base_res, "drawable")
create_app_icon(192, is_round=False).save(os.path.join(drawable_path, "ic_hamgam_launcher.png"), "PNG")

print("Successfully generated all raster PNG icons!")
