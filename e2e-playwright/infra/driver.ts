import { remote } from 'webdriverio';


export const mobileDriver = async (): Promise<WebdriverIO.Browser> => {
  const client = await remote({
    hostname: '127.0.0.1',
    port: 4723,
    path: '/wd/hub',
    logLevel: 'info',
    capabilities: {
      platformName: 'Android',
      'appium:automationName': 'UiAutomator2',
      'appium:deviceName': 'emulator-5554',
      'appium:app': '/workspace/app-debug.apk',
      'appium:newCommandTimeout': 300,
      'appium:autoGrantPermissions': true,
    },
  });

  return client;
};