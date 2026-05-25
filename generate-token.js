const jwt = require('jsonwebtoken');
const token = jwt.sign(
    { sub: 'b1@buyer.com', role: 'BUYER', userId: 1 },
    'bagify-super-secret-key-change-me-in-production-min-32-chars!!',
    { expiresIn: '1h' }
);
console.log(token);
