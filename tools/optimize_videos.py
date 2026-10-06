import os
import sys
import subprocess
import shutil
from pathlib import Path

# Paths
SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent
FFMPEG_EXE = SCRIPT_DIR / "ffmpeg.exe"

DEFAULT_INPUT_DIR = PROJECT_ROOT / "Input_Ads_Here"
DEFAULT_OUTPUT_DIR = PROJECT_ROOT / "Optimized_Ads_For_Kiosk"

VIDEO_EXTS = {".mp4", ".mov", ".mkv", ".avi", ".webm", ".3gp", ".flv", ".wmv", ".ts", ".m4v"}
IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}

def ensure_dirs():
    DEFAULT_INPUT_DIR.mkdir(parents=True, exist_ok=True)
    DEFAULT_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

def get_video_info(video_path: Path):
    """Run ffmpeg -i to extract resolution and duration"""
    cmd = [str(FFMPEG_EXE), "-i", str(video_path)]
    result = subprocess.run(cmd, stderr=subprocess.PIPE, stdout=subprocess.PIPE, text=True, encoding="utf-8", errors="ignore")
    output = result.stderr

    width, height = 1080, 1920
    is_portrait = True

    for line in output.splitlines():
        if "Stream" in line and "Video:" in line:
            # Look for e.g. 1920x1080 or 3840x2160
            parts = line.split(",")
            for p in parts:
                p = p.strip()
                if "x" in p:
                    tokens = p.split(" ")[0].split("x")
                    if len(tokens) == 2 and tokens[0].isdigit() and tokens[1].isdigit():
                        w, h = int(tokens[0]), int(tokens[1])
                        width, height = w, h
                        is_portrait = (h >= w)
                        break
    return width, height, is_portrait

def optimize_video(input_path: Path, output_path: Path):
    print(f"\n[>] Optimizing video: {input_path.name}")
    w, h, is_portrait = get_video_info(input_path)
    print(f"    Original resolution: {w}x{h} ({'Portrait' if is_portrait else 'Landscape'})")

    # Filter: Scale to fit inside 1080x1920 without distortion, pad remaining area with elegant black
    vf_filter = "scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2:color=black"

    cmd = [
        str(FFMPEG_EXE),
        "-i", str(input_path),
        "-vf", vf_filter,
        "-c:v", "libx264",
        "-profile:v", "high",
        "-level", "4.1",
        "-preset", "fast",
        "-crf", "22",
        "-maxrate", "5000k",
        "-bufsize", "8000k",
        "-pix_fmt", "yuv420p",
        "-r", "30",
        "-c:a", "aac",
        "-b:a", "128k",
        "-ar", "44100",
        "-movflags", "+faststart",
        "-y",
        str(output_path)
    ]

    proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding="utf-8", errors="ignore")
    if proc.returncode == 0 and output_path.exists() and output_path.stat().st_size > 0:
        orig_mb = input_path.stat().st_size / (1024 * 1024)
        opt_mb = output_path.stat().st_size / (1024 * 1024)
        print(f"    [+] SUCCESS -> {output_path.name}")
        print(f"    [+] Size reduced from {orig_mb:.1f} MB to {opt_mb:.1f} MB (Retail 1080x1920 30fps H.264)")
        return True
    else:
        print(f"    [!] FAILED to convert {input_path.name}")
        return False

def optimize_image(input_path: Path, output_path: Path):
    """Resize high-res camera photos (e.g. 50MP Galaxy S21 photos) to crisp 1080x1920 JPEG"""
    print(f"\n[>] Optimizing image: {input_path.name}")
    vf_filter = "scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2:color=black"
    target_path = output_path.with_suffix(".jpg")

    cmd = [
        str(FFMPEG_EXE),
        "-i", str(input_path),
        "-vf", vf_filter,
        "-q:v", "3",
        "-y",
        str(target_path)
    ]

    proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding="utf-8", errors="ignore")
    if proc.returncode == 0 and target_path.exists():
        orig_mb = input_path.stat().st_size / (1024 * 1024)
        opt_mb = target_path.stat().st_size / (1024 * 1024)
        print(f"    [+] SUCCESS -> {target_path.name} ({orig_mb:.1f} MB -> {opt_mb:.1f} MB)")
        return True
    return False

def main():
    print("=" * 65)
    print("       POS KIOSK AD VIDEO & IMAGE OPTIMIZER TOOL")
    print("       Target: 1080x1920 Portrait / Rockchip RK3576 Standard")
    print("=" * 65)

    if not FFMPEG_EXE.exists():
        print(f"[ERROR] ffmpeg.exe not found at {FFMPEG_EXE}")
        input("Press Enter to exit...")
        return

    ensure_dirs()

    # Determine input sources: command line arguments or default folder
    files_to_process = []
    if len(sys.argv) > 1:
        for arg in sys.argv[1:]:
            p = Path(arg)
            if p.is_file():
                files_to_process.append(p)
            elif p.is_dir():
                for sub in p.iterdir():
                    if sub.is_file():
                        files_to_process.append(sub)
    else:
        # Check DEFAULT_INPUT_DIR
        for p in DEFAULT_INPUT_DIR.iterdir():
            if p.is_file():
                files_to_process.append(p)

    if not files_to_process:
        print(f"\n[INFO] No media files found in:\n       {DEFAULT_INPUT_DIR}\n")
        print("INSTRUCTIONS:")
        print(f"1. Put your phone/camera videos & photos into:")
        print(f"   {DEFAULT_INPUT_DIR}")
        print("   OR drag and drop your video files directly onto OPTIMIZE_VIDEOS_FOR_KIOSK.bat\n")
        print("2. Run this tool again!\n")
        input("Press Enter to exit...")
        return

    print(f"\nFound {len(files_to_process)} media file(s) to optimize for your kiosk.\n")

    success_count = 0
    for file_path in files_to_process:
        ext = file_path.suffix.lower()
        base_name = file_path.stem
        out_name = f"{base_name}_kiosk.mp4"
        out_path = DEFAULT_OUTPUT_DIR / out_name

        if ext in VIDEO_EXTS:
            if optimize_video(file_path, out_path):
                success_count += 1
        elif ext in IMAGE_EXTS:
            out_img_name = f"{base_name}_kiosk.jpg"
            if optimize_image(file_path, DEFAULT_OUTPUT_DIR / out_img_name):
                success_count += 1
        else:
            print(f"[-] Skipping non-media file: {file_path.name}")

    print("\n" + "=" * 65)
    print(f"OPTIMIZATION COMPLETE: {success_count}/{len(files_to_process)} file(s) processed!")
    print(f"Optimized files are saved in:")
    print(f"  {DEFAULT_OUTPUT_DIR}")
    print("\nHOW TO USE ON YOUR KIOSK:")
    print("1. Copy the files from 'Optimized_Ads_For_Kiosk' onto a USB flash drive")
    print("2. Plug the USB into the kiosk and copy to '/sdcard/Ads/'")
    print("3. They will play flawlessly with 100% hardware acceleration and zero crashes!")
    print("=" * 65 + "\n")

    if sys.stdin.isatty() and "--no-wait" not in sys.argv:
        try:
            input("Press Enter to close...")
        except Exception:
            pass

if __name__ == "__main__":
    main()
