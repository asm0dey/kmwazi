#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Regenerates every raster launcher/Play icon from the SVG masters in this directory.
# Needs rsvg-convert and ImageMagick 7 (magick).
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
root="$(cd "$here/../.." && pwd)"
res="$root/app/src/main/res"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

for pair in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  dpi="${pair%%:*}" px="${pair##*:}"
  for name in ic_launcher ic_launcher_round; do
    src="$here/icon.svg"
    [[ $name == ic_launcher_round ]] && src="$here/icon-round.svg"
    rsvg-convert -w "$px" -h "$px" "$src" -o "$tmp/$name.png"
    magick "$tmp/$name.png" -define webp:lossless=true "$res/mipmap-$dpi/$name.webp"
  done
done

rsvg-convert -w 512 -h 512 "$here/play-512.svg" -o "$tmp/play.png"
for locale in en-US de-DE ru-RU; do
  magick "$tmp/play.png" -define png:color-type=6 "PNG32:$root/fastlane/metadata/android/$locale/images/icon.png"
done
cp "$root/fastlane/metadata/android/en-US/images/icon.png" "$root/app/src/main/ic_launcher-playstore.png"
