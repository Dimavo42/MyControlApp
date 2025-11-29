import { remote } from 'webdriverio';




export const mobileDriver = async () => {
    const myRemote:WebdriverIO.Browser = await remote({
        capabilities: {
            platformName: 'Android',
            'appium:deviceName': 'emulator-5554',
            'appium:platformVersion': '14',
            'appium:app': '/path/to/app.apk',
            'appium:automationName': 'UiAutomator2',
        },
    });

    return myRemote;
};