const fs = require('fs');
const path = require('path');
const QRCode = require('qrcode');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
const playStoreUrl = 'https://play.google.com/store/apps/details?id=com.pmd.userapp';

async function updateQrCode() {
    try {
        if (!fs.existsSync(htmlPath)) {
            console.error('HTML file not found!');
            return;
        }

        // Generate a high-resolution QR code (500x500 pixels) with high error correction
        const qrBase64DataUrl = await QRCode.toDataURL(playStoreUrl, {
            errorCorrectionLevel: 'H',
            margin: 2,
            width: 400,
            color: {
                dark: '#000000',
                light: '#FFFFFF'
            }
        });

        // The returned Data URL is in the format "data:image/png;base64,iVBORw0KG..."
        // We only want the base64 part or the whole Data URL?
        // Let's check how it's used in the HTML:
        // Line 591: <img src="data:image/png;base64,[BASE64_DATA]" ...
        // So we can extract the base64 part.
        const base64Part = qrBase64DataUrl.split(',')[1];
        console.log('Generated QR code successfully. Base64 length:', base64Part.length);

        let htmlContent = fs.readFileSync(htmlPath, 'utf8');
        const lines = htmlContent.split('\n');
        let replaced = false;

        for (let i = 0; i < lines.length; i++) {
            if (lines[i].includes('alt="Download QR Code"') && lines[i].includes('src="data:image/png;base64,')) {
                // Keep the exact styling and attributes, just replace the src attribute
                lines[i] = `                        <img src="data:image/png;base64,${base64Part}" style="width: 110px; height: 110px; border: 1px solid var(--border); border-radius: 8px;" alt="Download QR Code">`;
                replaced = true;
                console.log(`Replaced QR code image on line ${i + 1}.`);
                break;
            }
        }

        if (replaced) {
            htmlContent = lines.join('\n');
            fs.writeFileSync(htmlPath, htmlContent, 'utf8');
            console.log('Successfully updated HTML guide with the new high-resolution scan-ready QR code.');
        } else {
            console.error('Could not find QR code image tag in HTML!');
        }
    } catch (err) {
        console.error('Error updating QR code:', err);
    }
}

updateQrCode();
