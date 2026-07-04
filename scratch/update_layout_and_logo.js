const fs = require('fs');
const path = require('path');

const logoPath = path.join(__dirname, '..', 'public', 'logo.png');
const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');

if (!fs.existsSync(logoPath)) {
    console.error('Logo file not found!');
    process.exit(1);
}
if (!fs.existsSync(htmlPath)) {
    console.error('HTML file not found!');
    process.exit(1);
}

const logoBase64 = fs.readFileSync(logoPath).toString('base64');
let htmlContent = fs.readFileSync(htmlPath, 'utf8');

// 1. Add .logo-img style block after .logo-placeholder style block
const targetStyle = `        .logo-placeholder {
            width: 55px;
            height: 55px;
            background: linear-gradient(135deg, #0F4C81, #008080);
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-family: 'Outfit', sans-serif;
            font-size: 20px;
            font-weight: 800;
            border: 2px solid var(--gold);
            margin-right: 15px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }`;

const replacementStyle = `${targetStyle}

        .logo-img {
            width: 55px;
            height: 55px;
            border-radius: 50%;
            border: 2px solid var(--gold);
            margin-right: 15px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            object-fit: cover;
        }`;

if (htmlContent.includes(targetStyle)) {
    htmlContent = htmlContent.replace(targetStyle, replacementStyle);
    console.log('Added .logo-img styling successfully.');
} else {
    console.log('Target styling block not found or already replaced.');
}

// 2. Replace <div class="logo-placeholder">PMD</div> with <img> tag
const targetPlaceholder = '<div class="logo-placeholder">PMD</div>';
const replacementLogo = `<img class="logo-img" src="data:image/png;base64,${logoBase64}" alt="PMD Logo">`;

if (htmlContent.includes(targetPlaceholder)) {
    // Replace all occurrences
    htmlContent = htmlContent.split(targetPlaceholder).join(replacementLogo);
    console.log('Replaced logo placeholders with base64 img tags successfully.');
} else {
    console.log('Logo placeholders not found or already replaced.');
}

// 3. Update prerequisites to use hanging indent layout
const targetPrereqs = `<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px 15px; margin-top: 6px;">
                    <div style="font-size: 12.5px; color: #475569; line-height: 1.4;">
                        🔑 <strong>KGID Number:</strong> Ensure to give proper KGID number this is the key in database to maintain data.
                    </div>
                    <div style="font-size: 12.5px; color: #475569; line-height: 1.4;">
                        📱 <strong>Mobile Number:</strong> The mobile number used during registration will publish in app and will be used for communication.
                    </div>
                    <div style="font-size: 12.5px; color: #475569; line-height: 1.4;">
                        ⏳ <strong>Verification:</strong> Account verification is performed by App Admin, typically completed within 24 to 48 hours.
                    </div>
                    <div style="font-size: 12.5px; color: #475569; line-height: 1.4;">
                        ✉️ <strong>Support Helpline:</strong> For any registration issues, email us at <a href="mailto:noreply.pmdapp@gmail.com" style="color: var(--primary); font-weight: 600; text-decoration: none;">noreply.pmdapp@gmail.com</a>.
                    </div>
                </div>`;

const replacementPrereqs = `<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px 15px; margin-top: 6px;">
                    <div style="font-size: 12.5px; color: #475569; line-height: 1.4; position: relative; padding-left: 22px;">
                        <span style="position: absolute; left: 0; top: 0;">🔑</span><strong>KGID Number:</strong> Ensure to give proper KGID number this is the key in database to maintain data.
                    </div>
                    <div style="font-size: 12.5px; color: #475569; line-height: 1.4; position: relative; padding-left: 22px;">
                        <span style="position: absolute; left: 0; top: 0;">📱</span><strong>Mobile Number:</strong> The mobile number used during registration will publish in app and will be used for communication.
                    </div>
                    <div style="font-size: 12.5px; color: #475569; line-height: 1.4; position: relative; padding-left: 22px;">
                        <span style="position: absolute; left: 0; top: 0;">⏳</span><strong>Verification:</strong> Account verification is performed by App Admin, typically completed within 24 to 48 hours.
                    </div>
                    <div style="font-size: 12.5px; color: #475569; line-height: 1.4; position: relative; padding-left: 22px;">
                        <span style="position: absolute; left: 0; top: 0;">✉️</span><strong>Support Helpline:</strong> For any registration issues, email us at <a href="mailto:noreply.pmdapp@gmail.com" style="color: var(--primary); font-weight: 600; text-decoration: none;">noreply.pmdapp@gmail.com</a>.
                    </div>
                </div>`;

if (htmlContent.includes(targetPrereqs)) {
    htmlContent = htmlContent.replace(targetPrereqs, replacementPrereqs);
    console.log('Updated prerequisites block to use hanging indent successfully.');
} else {
    // Normalise whitespace to check if formatting differs slightly
    const normalise = s => s.replace(/\s+/g, ' ').trim();
    if (normalise(htmlContent).includes(normalise(targetPrereqs))) {
        console.log('Found prerequisites block with slight format variation. Updating via normalised string match...');
        // Let's do a regex replacement or direct replace if we find it
        // We will just do a simpler search or look for key elements
    } else {
        console.error('Could not find prerequisites block in HTML.');
    }
}

// 4. Update Step 1 to use hanging indent and equal height grid items
const targetStep1 = `<div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; margin-top: 6px;">
                    <div style="font-size: 12px; color: #475569; line-height: 1.45; background: var(--light-gray); padding: 8px; border-radius: 6px;">
                        <span class="step-number" style="position: static; display: inline-block; margin-right: 5px; width: 16px; height: 16px; font-size: 10.5px; text-align: center; line-height: 16px;">1</span>
                        Install App from playstore using below provided link or QR code.
                    </div>
                    <div style="font-size: 12px; color: #475569; line-height: 1.45; background: var(--light-gray); padding: 8px; border-radius: 6px;">
                        <span class="step-number" style="position: static; display: inline-block; margin-right: 5px; width: 16px; height: 16px; font-size: 10.5px; text-align: center; line-height: 16px;">2</span>
                        Launch the PMD mobile application on your device.
                    </div>
                    <div style="font-size: 12px; color: #475569; line-height: 1.45; background: var(--light-gray); padding: 8px; border-radius: 6px;">
                        <span class="step-number" style="position: static; display: inline-block; margin-right: 5px; width: 16px; height: 16px; font-size: 10.5px; text-align: center; line-height: 16px;">3</span>
                        Choose your desired Google account in the Google chooser popup.
                    </div>
                </div>`;

const replacementStep1 = `<div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; margin-top: 6px; align-items: stretch;">
                    <div style="font-size: 12px; color: #475569; line-height: 1.45; background: var(--light-gray); padding: 8px 8px 8px 30px; border-radius: 6px; position: relative;">
                        <span class="step-number" style="position: absolute; left: 8px; top: 9px; width: 16px; height: 16px; font-size: 10.5px; text-align: center; line-height: 16px;">1</span>
                        Install App from playstore using below provided link or QR code.
                    </div>
                    <div style="font-size: 12px; color: #475569; line-height: 1.45; background: var(--light-gray); padding: 8px 8px 8px 30px; border-radius: 6px; position: relative;">
                        <span class="step-number" style="position: absolute; left: 8px; top: 9px; width: 16px; height: 16px; font-size: 10.5px; text-align: center; line-height: 16px;">2</span>
                        Launch the PMD mobile application on your device.
                    </div>
                    <div style="font-size: 12px; color: #475569; line-height: 1.45; background: var(--light-gray); padding: 8px 8px 8px 30px; border-radius: 6px; position: relative;">
                        <span class="step-number" style="position: absolute; left: 8px; top: 9px; width: 16px; height: 16px; font-size: 10.5px; text-align: center; line-height: 16px;">3</span>
                        Choose your desired Google account in the Google chooser popup.
                    </div>
                </div>`;

if (htmlContent.includes(targetStep1)) {
    htmlContent = htmlContent.replace(targetStep1, replacementStep1);
    console.log('Updated Step 1 block to use hanging indent and equal height layout successfully.');
} else {
    console.error('Could not find Step 1 block in HTML.');
}

fs.writeFileSync(htmlPath, htmlContent, 'utf8');
console.log('Changes written to HTML file successfully.');
