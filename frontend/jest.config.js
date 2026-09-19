const preset = require('jest-preset-angular/presets').createCjsPreset({
  tsconfig: '<rootDir>/tsconfig.spec.json'
});

module.exports = {
  ...preset,
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testMatch: ['<rootDir>/src/**/*.spec.ts'],
  moduleNameMapper: {
    '^src/(.*)$': '<rootDir>/src/$1'
  },
  transformIgnorePatterns: [
    'node_modules/(?!.*\\.mjs$|@angular|@ngx-translate|ng2-charts|lodash-es|chart\\.js)'
  ]
};
