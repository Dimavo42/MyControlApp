import { remote } from 'webdriverio';
import path from 'path';

export const mobileDriver = async (): Promise<WebdriverIO.Browser> => {
  //for local debug
  // const apkPath = path.resolve(
  //   __dirname,
  //   '..',
  //   '..',
  //   'app',
  //   'build',
  //   'outputs',
  //   'apk',
  //   'debug',
  //   'app-debug.apk'
  // );
  const apkPath = '/workspace/app-debug.apk';
  const client = await remote({
    hostname: '127.0.0.1',
    port: 4723,
    path: '/',
    logLevel: 'info',
    capabilities: {
      platformName: 'Android',
      'appium:automationName': 'UiAutomator2',
      'appium:deviceName': 'emulator-5554',
      'appium:app': apkPath,
      'appium:newCommandTimeout': 300,
      'appium:autoGrantPermissions': true,
    },
  });

  return client;
};