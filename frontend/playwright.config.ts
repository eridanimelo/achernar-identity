import { defineConfig } from '@playwright/test';
export default defineConfig({testDir:'./e2e',workers:1,timeout:60000,use:{baseURL:'http://localhost:4200',headless:true,trace:'retain-on-failure',screenshot:'only-on-failure'},reporter:[['list']],outputDir:'test-results'});
