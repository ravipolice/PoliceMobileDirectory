try {
    const qr = require('qrcode');
    console.log('qrcode library is installed!');
} catch (e) {
    console.log('qrcode library is NOT installed.');
}

try {
    const qrImage = require('qr-image');
    console.log('qr-image library is installed!');
} catch (e) {
    console.log('qr-image library is NOT installed.');
}
