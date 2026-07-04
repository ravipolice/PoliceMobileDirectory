import os
from PIL import Image

def crop_image():
    input_path = os.path.join(os.path.dirname(__file__), 'mockup.jpg')
    output_path = os.path.join(os.path.dirname(__file__), 'mockup_cropped.jpg')
    
    if not os.path.exists(input_path):
        print(f"Error: {input_path} not found")
        return
        
    img = Image.open(input_path)
    width, height = img.size
    print(f"Original size: {width}x{height}")
    
    # We want to crop the image to remove the top and bottom white/light-gray space.
    # Let's inspect the pixels to find the boundaries.
    # The phone mockup starts with a green header at the top of the phone screen, 
    # and has a card layout with shadows.
    # Let's find the bounding box by scanning from top/bottom/left/right for pixels that are NOT 
    # the background color.
    # Background color seems to be very close to white or light gray.
    # Let's look at the top-left pixel to get the background color.
    bg_color = img.getpixel((0, 0))
    print(f"Detected background color at (0,0): {bg_color}")
    
    # We will scan from top to bottom to find where the phone mockup starts and ends.
    # Since it might have a shadow or some soft border, we'll use a threshold.
    threshold = 15
    
    def is_bg(pixel):
        if isinstance(pixel, int):  # Grayscale image
            return abs(pixel - bg_color) < threshold
        # RGB image
        return all(abs(pixel[i] - bg_color[i]) < threshold for i in range(min(len(pixel), 3)))

    top = 0
    for y in range(height):
        row_has_content = False
        for x in range(width):
            if not is_bg(img.getpixel((x, y))):
                row_has_content = True
                break
        if row_has_content:
            top = y
            break
            
    bottom = height - 1
    for y in range(height - 1, -1, -1):
        row_has_content = False
        for x in range(width):
            if not is_bg(img.getpixel((x, y))):
                row_has_content = True
                break
        if row_has_content:
            bottom = y
            break
            
    left = 0
    for x in range(width):
        col_has_content = False
        for y in range(height):
            if not is_bg(img.getpixel((x, y))):
                col_has_content = True
                break
        if col_has_content:
            left = x
            break
            
    right = width - 1
    for x in range(width - 1, -1, -1):
        col_has_content = False
        for y in range(height):
            if not is_bg(img.getpixel((x, y))):
                col_has_content = True
                break
        if col_has_content:
            right = x
            break
            
    print(f"Content boundaries - Left: {left}, Top: {top}, Right: {right}, Bottom: {bottom}")
    
    # Add a small padding of 5 pixels around the phone screen to not clip the shadow
    padding = 10
    crop_left = max(0, left - padding)
    crop_top = max(0, top - padding)
    crop_right = min(width, right + padding)
    crop_bottom = min(height, bottom + padding)
    
    cropped_img = img.crop((crop_left, crop_top, crop_right, crop_bottom))
    cropped_img.save(output_path, "JPEG", quality=95)
    print(f"Cropped image saved to: {output_path}")
    print(f"New size: {cropped_img.size[0]}x{cropped_img.size[1]}")

if __name__ == '__main__':
    crop_image()
