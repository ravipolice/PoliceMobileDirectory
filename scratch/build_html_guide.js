const fs = require('fs');
const path = require('path');

const brainDir = "C:/Users/ravip/.gemini/antigravity/brain/cc2a4ac8-491f-4ef1-8e68-260091d4b65b";
const artifactsDir = path.join(brainDir, "artifacts");

const loginImgPath = path.join(artifactsDir, "login_screen.png");
const regImgPath = path.join(artifactsDir, "registration_form.png");

console.log("Loading images...");
let loginBase64 = "";
let regBase64 = "";

try {
    if (fs.existsSync(loginImgPath)) {
        loginBase64 = fs.readFileSync(loginImgPath).toString('base64');
    }
    if (fs.existsSync(regImgPath)) {
        regBase64 = fs.readFileSync(regImgPath).toString('base64');
    }
} catch (e) {
    console.error("Error reading images:", e);
}

const htmlContent = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Police Mobile Directory - Registration Guide</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Outfit:wght@500;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #0F4C81;
            --primary-light: #EBF3F9;
            --accent: #008080;
            --dark: #1E293B;
            --light-gray: #F8FAFC;
            --border: #E2E8F0;
            --gold: #D4AF37;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Inter', sans-serif;
            color: var(--dark);
            background-color: #64748B;
            line-height: 1.5;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
        }

        /* A4 Page Formatting */
        .page {
            width: 210mm;
            height: 297mm;
            padding: 15mm;
            margin: 20px auto;
            background: white;
            box-shadow: 0 10px 25px rgba(0,0,0,0.3);
            position: relative;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
        }

        @media print {
            body {
                background: none;
            }
            .page {
                margin: 0;
                box-shadow: none;
                page-break-after: always;
                width: 210mm;
                height: 297mm;
            }
        }

        /* Header Styling */
        .header {
            display: flex;
            align-items: center;
            border-bottom: 3px solid var(--primary);
            padding-bottom: 8px;
            margin-bottom: 15px;
        }

        .logo-placeholder {
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
        }

        .title-area h1 {
            font-family: 'Outfit', sans-serif;
            font-size: 22px;
            color: var(--primary);
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .title-area p {
            font-size: 12px;
            color: #64748B;
            font-weight: 500;
        }

        /* Page Layouts */
        .content-grid {
            display: grid;
            grid-template-columns: 1.1fr 0.9fr;
            gap: 20px;
            flex-grow: 1;
        }

        .column-left {
            display: flex;
            flex-direction: column;
            gap: 15px;
        }

        .column-right {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            background: var(--light-gray);
            border-radius: 12px;
            border: 1px dashed var(--border);
            padding: 10px;
        }

        .mockup-img {
            max-width: 100%;
            max-height: 230mm;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            border: 1px solid var(--border);
            object-fit: contain;
        }

        /* Section Cards */
        .section-card {
            background: white;
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 12px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.02);
        }

        .section-title {
            font-family: 'Outfit', sans-serif;
            font-size: 14px;
            color: var(--primary);
            border-left: 4px solid var(--accent);
            padding-left: 8px;
            margin-bottom: 8px;
            text-transform: uppercase;
            font-weight: 700;
        }

        /* Step Lists */
        .step-list {
            list-style: none;
        }

        .step-item {
            position: relative;
            padding-left: 28px;
            margin-bottom: 10px;
            font-size: 12px;
        }

        .step-number {
            position: absolute;
            left: 0;
            top: 1px;
            width: 18px;
            height: 18px;
            background: var(--primary-light);
            color: var(--primary);
            border-radius: 50%;
            font-size: 10px;
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .step-item strong {
            color: var(--primary);
        }

        /* Tables */
        .field-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 10.5px;
            margin-top: 5px;
        }

        .field-table th, .field-table td {
            border: 1px solid var(--border);
            padding: 6px 8px;
            text-align: left;
        }

        .field-table th {
            background-color: var(--primary-light);
            color: var(--primary);
            font-weight: 600;
            text-transform: uppercase;
            font-size: 9.5px;
        }

        .field-table tr:nth-child(even) {
            background-color: var(--light-gray);
        }

        .req-badge {
            background-color: #FEE2E2;
            color: #EF4444;
            padding: 1px 4px;
            border-radius: 4px;
            font-size: 9px;
            font-weight: 700;
        }

        .opt-badge {
            background-color: #F1F5F9;
            color: #64748B;
            padding: 1px 4px;
            border-radius: 4px;
            font-size: 9px;
            font-weight: 500;
        }

        /* Flowchart styling */
        .flow-container {
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: var(--light-gray);
            border-radius: 8px;
            padding: 8px 12px;
            border: 1px solid var(--border);
            margin-top: 5px;
        }

        .flow-node {
            background: white;
            border: 1.5px solid var(--primary);
            border-radius: 6px;
            padding: 5px 8px;
            font-size: 10.5px;
            font-weight: 600;
            text-align: center;
            box-shadow: 0 2px 4px rgba(0,0,0,0.03);
        }

        .flow-node.highlight {
            border-color: var(--accent);
            background: var(--primary-light);
        }

        .flow-arrow {
            color: var(--primary);
            font-weight: 700;
            font-size: 14px;
        }

        /* Footer styling */
        .footer {
            border-top: 1px solid var(--border);
            padding-top: 8px;
            margin-top: 10px;
            display: flex;
            justify-content: space-between;
            font-size: 10px;
            color: #94A3B8;
        }

        .footer-logo {
            font-family: 'Outfit', sans-serif;
            font-weight: 700;
            color: var(--primary);
        }

        /* Callouts */
        .callout {
            background-color: var(--primary-light);
            border-left: 4px solid var(--primary);
            padding: 10px;
            border-radius: 0 8px 8px 0;
            font-size: 11px;
            color: #0F4C81;
            margin-top: 5px;
        }

        .callout-warning {
            background-color: #FEF2F2;
            border-left-color: #EF4444;
            color: #991B1B;
        }

        /* Edit Layout Styles */
        body {
            padding-top: 50px;
        }
        .edit-toolbar {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            height: 50px;
            background: rgba(30, 41, 59, 0.95);
            backdrop-filter: blur(5px);
            z-index: 9999;
            display: flex;
            align-items: center;
            padding: 0 20px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            font-family: 'Inter', sans-serif;
        }
        .edit-toolbar button {
            background: var(--primary);
            color: white;
            border: none;
            padding: 6px 14px;
            border-radius: 6px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            margin-right: 10px;
            transition: all 0.2s;
        }
        .edit-toolbar button:hover {
            background: var(--accent);
        }
        .edit-active .draggable-item {
            position: absolute !important;
            cursor: move;
            resize: both;
            overflow: auto;
            border: 1.5px dashed var(--accent) !important;
            box-shadow: 0 8px 16px rgba(0,0,0,0.15) !important;
            z-index: 10;
            background: white;
        }
        .edit-active .draggable-item::after {
            content: "✥ Drag Header to Move / Resize Bottom-Right Corner ⤨";
            display: block;
            font-size: 8px;
            color: var(--accent);
            text-align: right;
            margin-top: 4px;
            font-weight: bold;
        }
        .drag-handle {
            display: none;
        }
        .edit-active .drag-handle {
            display: block !important;
        }
        @media print {
            .no-print {
                display: none !important;
            }
            body {
                background: none !important;
                padding-top: 0 !important;
            }
        }
    </style>
</head>
<body>
    <div class="edit-toolbar no-print">
        <button onclick="toggleEditMode()" id="editToggleBtn">🔧 Edit Layout</button>
        <button onclick="resetLayout()">🔄 Reset Layout</button>
        <span style="margin-left: 15px; font-size: 12px; color: #E2E8F0;">Click <strong>Edit Layout</strong> to drag boxes by their headers or resize them from bottom-right. Save or reset anytime.</span>
      <!-- PAGE 1: LOGIN & SIGN IN WALKTHROUGH -->
    <div class="page">
        <div>
            <div class="header">
                <div class="logo-placeholder">PMD</div>
                <div class="title-area">
                    <h1>Police Mobile Directory</h1>
                    <p>Officer & Employee Mobile App Registration Guide (Page 1 of 2)</p>
                </div>
            </div>

            <div class="content-grid">
                <div class="column-left">
                    <div class="section-card draggable-item" id="p1-overview">
                        <div class="section-title">Overview</div>
                        <p style="font-size: 12px; color: #475569; margin-bottom: 5px;">
                            The **Police Mobile Directory (PMD)** app is a secure platform restricted to verified Karnataka State Police Department personnel. 
                            Use this guide to authorize your account and register successfully.
                        </p>
                    </div>

                    <div class="section-card draggable-item" id="p1-step1">
                        <div class="section-title">Step 1: Link Google Account</div>
                        <ul class="step-list">
                            <li class="step-item">
                                <span class="step-number">1</span>
                                Launch the **PMD** mobile application on your device.
                            </li>
                            <li class="step-item">
                                <span class="step-number">2</span>
                                Tap the primary **Sign in with Google / Register** button.
                            </li>
                            <li class="step-item">
                                <span class="step-number">3</span>
                                Choose your official/desired Google account in the Google chooser popup.
                            </li>
                        </ul>
                    </div>

                    <div class="section-card draggable-item" id="p1-step2">
                        <div class="section-title">Step 2: Submit Registration Dialog</div>
                        <ul class="step-list">
                            <li class="step-item">
                                <span class="step-number">4</span>
                                If your Google account is unrecognized, a dialog prompt will ask: 
                                *'Do you wish to submit a registration request?'*
                            </li>
                            <li class="step-item">
                                <span class="step-number">5</span>
                                Verify the email matches, then tap **Register** to open the registration form.
                            </li>
                        </ul>
                    </div>

                    <div class="section-card draggable-item" id="p1-alternative-login">
                        <div class="section-title">Alternative Login Option</div>
                        <p style="font-size: 11px; color: #475569; margin-bottom: 5px;">
                            For offline access, you can use the **Email and PIN login card**. Note the **"For Offline Use"** label positioned directly above the card.
                        </p>
                        <div class="callout">
                            <strong>Note:</strong> PIN sign-in is only available after your initial Google registration has been approved by the administrators.
                        </div>
                    </div>

                    <!-- PMD DASHBOARD FEATURES -->
                    <div class="section-card draggable-item" id="p1-dashboard-header" style="border-color: var(--accent);">
                        <div class="section-title" style="color: var(--accent); border-left-color: var(--primary);">PMD Dashboard Features</div>
                        <p style="font-size: 11px; color: #475569; line-height: 1.35;">
                            Once registered and authorized, users gain access to a comprehensive suite of tools directly from the PMD Dashboard.
                        </p>
                    </div>

                    <div class="section-card draggable-item" id="p1-leave-manager">
                        <div class="section-title">Leave Manager</div>
                        <p style="font-size: 10.5px; color: #475569; margin-bottom: 4px;">
                            Maintain leave records, and view available balances for:
                        </p>
                        <ul style="font-size: 10px; padding-left: 15px; color: #475569; line-height: 1.35; margin-bottom: 2px;">
                            <li>Earned Leave (EL)</li>
                            <li>Half Pay Leave (HPL)</li>
                            <li>Casual Leave (CL)</li>
                        </ul>
                    </div>

                    <div class="section-card draggable-item" id="p1-payslip-parser">
                        <div class="section-title">AI Pay Slip Parser</div>
                        <p style="font-size: 10.5px; color: #475569; margin-bottom: 4px;">
                            Securely import and analyze salary payslips using AI-powered parsing. The system:
                        </p>
                        <ul style="font-size: 10px; padding-left: 15px; color: #475569; line-height: 1.35; margin-bottom: 2px;">
                            <li>Extracts and organizes pay-related information automatically.</li>
                            <li>Stores payslip images securely as backup copies in Google Drive.</li>
                            <li>Maintains structured salary data in Excel format and backups in Google Drive.</li>
                        </ul>
                    </div>

                    <div class="section-card draggable-item" id="p1-nudi-converter">
                        <div class="section-title">Nudi Converter</div>
                        <p style="font-size: 10.5px; color: #475569; margin-bottom: 4px;">
                            A versatile language conversion utility that:
                        </p>
                        <ul style="font-size: 10px; padding-left: 15px; color: #475569; line-height: 1.35; margin-bottom: 2px;">
                            <li>Converts Kannada Unicode text to Nudi (ASCII) format and vice versa.</li>
                            <li>Supports TXT, PDF, and DOCX documents.</li>
                            <li>Enables both text conversion and complete file conversion.</li>
                        </ul>
                    </div>

                    <div class="section-card draggable-item" id="p1-gallery">
                        <div class="section-title">Gallery</div>
                        <p style="font-size: 10.5px; color: #475569; line-height: 1.35;">
                            Browse and access departmental event photographs, official media, and image galleries.
                        </p>
                    </div>

                    <div class="section-card draggable-item" id="p1-documents">
                        <div class="section-title">Documents & Circulars</div>
                        <p style="font-size: 10.5px; color: #475569; margin-bottom: 4px;">
                            Download, view, and search official departmental documents, including GOs, circulars, rules, and forms.
                        </p>
                    </div>

                    <div class="section-card draggable-item" id="p1-useful-links">
                        <div class="section-title">Useful Links</div>
                        <p style="font-size: 10.5px; color: #475569; margin-bottom: 4px;">
                            Quick access to important KSP Applications, Karnataka State Portals, and Central Govt Services.
                        </p>
                    </div>

                    <div class="section-card draggable-item" id="p1-additional-utilities" style="background-color: var(--light-gray);">
                        <div class="section-title" style="color: #64748B; border-left-color: #64748B;">Additional Utilities</div>
                        <p style="font-size: 10px; color: #64748B; line-height: 1.35;">
                            The dashboard may also include future productivity and communication enhancements for authorized personnel.
                        </p>
                    </div>

                </div>

                <div class="column-right draggable-item" id="p1-column-right">
                    <div class="drag-handle" style="width: 100%; text-align: center; font-size: 10px; background: var(--primary-light); color: var(--primary); padding: 3px 0; border-radius: 6px 6px 0 0; font-weight: bold; cursor: move; margin-bottom: 5px;">✥ Drag Column</div>
                    <p style="font-size: 11px; font-weight:600; color:var(--primary); margin-bottom:8px;">App Login Interface</p>
                    <img class="mockup-img" src="data:image/png;base64,${loginBase64}" alt="PMD Login Interface">
                </div>
            </div>
        </div>

        <div class="footer">
            <span>Official KSP Directory User Guide</span>
            <span class="footer-logo">PMD CONNECTING CLOSER</span>
            <span>Page 1</span>
        </div>
    </div>

    <!-- PAGE 2: REGISTRATION FORM & APPROVAL -->
    <div class="page">
        <div>
            <div class="header">
                <div class="logo-placeholder">PMD</div>
                <div class="title-area">
                    <h1>Police Mobile Directory</h1>
                    <p>Officer & Employee Mobile App Registration Guide (Page 2 of 2)</p>
                </div>
            </div>

            <div class="content-grid">
                <div class="column-left">
                    <div class="section-card draggable-item" id="p2-step3">
                        <div class="section-title">Step 3: Fill Registration Form</div>
                        <p style="font-size: 11px; color: #475569; margin-bottom: 8px;">
                            Fill out the required information with absolute accuracy to ensure quick administrative approval:
                        </p>

                        <table class="field-table">
                            <thead>
                                <tr>
                                    <th style="width: 25%;">Field</th>
                                    <th style="width: 20%;">Requirement</th>
                                    <th>Description & Validation Rules</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td><strong>Profile Photo</strong></td>
                                    <td><span class="req-badge">REQUIRED</span></td>
                                    <td>Tap "Add Photo" to snap a camera shot or upload from gallery.</td>
                                </tr>
                                <tr>
                                    <td><strong>Name</strong></td>
                                    <td><span class="req-badge">REQUIRED</span></td>
                                    <td>Must match your official service record name.</td>
                                </tr>
                                <tr>
                                    <td><strong>Email</strong></td>
                                    <td><span class="req-badge">LOCKED</span></td>
                                    <td>Automatically filled from Google sign-in and disabled.</td>
                                </tr>
                                <tr>
                                    <td><strong>Mobile 1</strong></td>
                                    <td><span class="req-badge">REQUIRED</span></td>
                                    <td>Enter active 10 to 13 digit phone number.</td>
                                </tr>
                                <tr>
                                    <td><strong>Mobile 2</strong></td>
                                    <td><span class="opt-badge">OPTIONAL</span></td>
                                    <td>Secondary mobile contact.</td>
                                </tr>
                                <tr>
                                    <td><strong>Unit & District</strong></td>
                                    <td><span class="req-badge">REQUIRED</span></td>
                                    <td>Dropdown selection. Options filter automatically based on unit.</td>
                                </tr>
                                <tr>
                                    <td><strong>KGID & Rank</strong></td>
                                    <td><span class="req-badge">REQUIRED</span></td>
                                    <td>Unique govt insurance number & service rank dropdown.</td>
                                </tr>
                                <tr>
                                    <td><strong>Station / Branch</strong></td>
                                    <td><span class="req-badge">REQUIRED</span></td>
                                    <td>Lists stations inside your District. Use "Others" if not listed.</td>
                                </tr>
                                <tr>
                                    <td><strong>Blood Group</strong></td>
                                    <td><span class="req-badge">REQUIRED</span></td>
                                    <td>Select your blood group. Visible on your profile card.</td>
                                </tr>
                                <tr>
                                    <td><strong>6-Digit PIN</strong></td>
                                    <td><span class="req-badge">REQUIRED</span></td>
                                    <td>Create and confirm your numeric PIN for quick sign-in.</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                    <div class="section-card draggable-item" id="p2-step4">
                        <div class="section-title">Step 4: Submission & Admin Approval</div>
                        <p style="font-size: 11.5px; color: #475569;">
                            Check the **Terms and Conditions** checkbox and tap **Submit for approval**.
                        </p>
                        
                        <div class="flow-container">
                            <div class="flow-node">Submit Form</div>
                            <div class="flow-arrow">➔</div>
                            <div class="flow-node highlight">Admin Audit</div>
                            <div class="flow-arrow">➔</div>
                            <div class="flow-node">Approved Login</div>
                        </div>

                        <ul style="margin-top: 8px; font-size: 11px; padding-left: 15px; color: #475569;">
                            <li><strong>Pending:</strong> Displays a *'Pending approval'* notification upon login.</li>
                            <li><strong>Rejected:</strong> Displays reasons in red text. Tap *'Register Again'* to fix.</li>
                        </ul>
                    </div>

                    <div class="callout callout-warning draggable-item" id="p2-warning">
                        <strong>Security Warning:</strong>
                        Sharing application credentials or exporting database listings is strictly prohibited and subject to department disciplinary action.
                    </div>
                </div>

                <div class="column-right draggable-item" id="p2-column-right">
                    <div class="drag-handle" style="width: 100%; text-align: center; font-size: 10px; background: var(--primary-light); color: var(--primary); padding: 3px 0; border-radius: 6px 6px 0 0; font-weight: bold; cursor: move; margin-bottom: 5px;">✥ Drag Column</div>
                    <p style="font-size: 11px; font-weight:600; color:var(--primary); margin-bottom:8px;">App Registration Form</p>
                    <img class="mockup-img" src="data:image/png;base64,${regBase64}" alt="PMD Registration Form">
                </div>
            </div>
        </div>

        <div class="footer">
            <span>Official KSP Directory User Guide</span>
            <span class="footer-logo">PMD CONNECTING CLOSER</span>
            <span>Page 2</span>
        </div>
    </div>

    <script>
        let editMode = false;
        let activeDragItem = null;
        let dragStartX = 0;
        let dragStartY = 0;
        let itemStartX = 0;
        let itemStartY = 0;

        window.addEventListener('DOMContentLoaded', () => {
            loadSavedLayout();
        });

        function saveLayout() {
            const layoutData = {};
            document.querySelectorAll('.draggable-item').forEach((item, index) => {
                const id = item.id || 'item-' + index;
                if (!item.id) item.id = id;
                layoutData[id] = {
                    left: item.style.left,
                    top: item.style.top,
                    width: item.style.width,
                    height: item.style.height,
                    position: item.style.position
                };
            });
            localStorage.setItem('pmd_user_guide_layout', JSON.stringify(layoutData));
        }

        function loadSavedLayout() {
            const dataStr = localStorage.getItem('pmd_user_guide_layout');
            if (!dataStr) return;
            try {
                const layoutData = JSON.parse(dataStr);
                document.querySelectorAll('.draggable-item').forEach((item, index) => {
                    const id = item.id || 'item-' + index;
                    if (!item.id) item.id = id;
                    const saved = layoutData[id];
                    if (saved) {
                        item.style.position = saved.position || 'absolute';
                        item.style.left = saved.left;
                        item.style.top = saved.top;
                        item.style.width = saved.width;
                        item.style.height = saved.height;
                        item.style.margin = '0';
                        
                        item.dataset.origStyle = item.getAttribute('style') || '';
                    }
                });
            } catch (e) {
                console.error("Error loading layout:", e);
            }
        }

        function toggleEditMode() {
            editMode = !editMode;
            const body = document.body;
            const btn = document.getElementById('editToggleBtn');
            
            if (editMode) {
                body.classList.add('edit-active');
                btn.innerHTML = "💾 Save Layout";
                btn.style.background = "#22C55E";
                
                // Measure all layout items first to prevent collapse shift
                const positions = [];
                document.querySelectorAll('.draggable-item').forEach(item => {
                    const page = item.closest('.page');
                    const pageRect = page.getBoundingClientRect();
                    const itemRect = item.getBoundingClientRect();
                    positions.push({
                        item: item,
                        left: itemRect.left - pageRect.left,
                        top: itemRect.top - pageRect.top,
                        width: itemRect.width,
                        height: itemRect.height
                    });
                });
                
                // Apply absolute coordinates after all items are measured
                positions.forEach(pos => {
                    const item = pos.item;
                    if (!item.dataset.origStyle) {
                        item.dataset.origStyle = item.getAttribute('style') || '';
                        item.dataset.origLeft = pos.left;
                        item.dataset.origTop = pos.top;
                        item.dataset.origWidth = pos.width;
                        item.dataset.origHeight = pos.height;
                    }
                    
                    item.style.position = 'absolute';
                    item.style.left = pos.left + 'px';
                    item.style.top = pos.top + 'px';
                    item.style.width = pos.width + 'px';
                    item.style.height = pos.height + 'px';
                    item.style.margin = '0';
                });
            } else {
                body.classList.remove('edit-active');
                btn.innerHTML = "🔧 Edit Layout";
                btn.style.background = "";
                saveLayout();
            }
        }

        function resetLayout() {
            localStorage.removeItem('pmd_user_guide_layout');
            document.querySelectorAll('.draggable-item').forEach(item => {
                item.style.position = '';
                item.style.left = '';
                item.style.top = '';
                item.style.width = '';
                item.style.height = '';
                item.style.margin = '';
                if (item.dataset.origStyle !== undefined) {
                    item.setAttribute('style', item.dataset.origStyle);
                    delete item.dataset.origStyle;
                }
            });
            if (editMode) {
                editMode = false;
                document.body.classList.remove('edit-active');
                const btn = document.getElementById('editToggleBtn');
                btn.innerHTML = "🔧 Edit Layout";
                btn.style.background = "";
            }
        }

        document.addEventListener('mousedown', e => {
            if (!editMode) return;
            
            const dragHeader = e.target.closest('.section-title, .callout, .callout-warning, .drag-handle');
            if (!dragHeader) return;
            
            const item = dragHeader.closest('.draggable-item');
            if (!item) return;
            
            if (e.offsetX > dragHeader.clientWidth || e.offsetY > dragHeader.clientHeight) return;
            
            activeDragItem = item;
            dragStartX = e.clientX;
            dragStartY = e.clientY;
            itemStartX = parseFloat(item.style.left) || 0;
            itemStartY = parseFloat(item.style.top) || 0;
            
            e.preventDefault();
        });

        document.addEventListener('mousemove', e => {
            if (!editMode || !activeDragItem) return;
            
            const dx = e.clientX - dragStartX;
            const dy = e.clientY - dragStartY;
            
            activeDragItem.style.left = (itemStartX + dx) + 'px';
            activeDragItem.style.top = (itemStartY + dy) + 'px';
        });

        document.addEventListener('mouseup', () => {
            activeDragItem = null;
        });
    </script>
`;

const htmlDest = "c:/Users/ravip/AndroidStudioProjects/PoliceMobileDirectory/user_registration_guide.html";
const htmlDestBrain = path.join(artifactsDir, "user_registration_guide.html");
try {
    fs.writeFileSync(htmlDest, htmlContent, 'utf-8');
    fs.writeFileSync(htmlDestBrain, htmlContent, 'utf-8');
    console.log(`Successfully compiled self-contained A4 guide HTML: ${htmlDest}`);
} catch (e) {
    console.error("Error writing HTML:", e);
}
