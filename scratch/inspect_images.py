import os
from PIL import Image

assets_dir = r"c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory\assets"
print(f"Inspecting assets in: {assets_dir}")

if not os.path.exists(assets_dir):
    print("Assets directory does not exist!")
    exit(1)

files = os.listdir(assets_dir)
print(f"Total files: {len(files)}")

for i, f in enumerate(files):
    if f.lower().endswith(('.png', '.jpg', '.jpeg')):
        full_path = os.path.join(assets_dir, f)
        try:
            with Image.open(full_path) as img:
                print(f"[{i}] {f}")
                print(f"    Size: {os.path.getsize(full_path)} bytes")
                print(f"    Format: {img.format}, Mode: {img.mode}, Dimensions: {img.size}")
        except Exception as e:
            print(f"[{i}] {f} - Error: {e}")
