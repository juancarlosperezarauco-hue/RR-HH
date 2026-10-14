const fs = require('fs');

const inDocker = fs.existsSync('/.dockerenv');
const target = process.env.BACKEND_URL || (inDocker ? 'http://serv-hrcore-api:8080' : 'http://localhost:8080');

console.log(`[Proxy] Routing /api requests to target: ${target}`);

const PROXY_CONFIG = {
  '/api': {
    target: target,
    secure: false,
    changeOrigin: true,
    logLevel: 'debug'
  }
};

module.exports = PROXY_CONFIG;
