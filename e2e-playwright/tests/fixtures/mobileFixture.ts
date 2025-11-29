import { test as base, expect } from '@playwright/test';
import { mobileDriver } from '../../infra/driver';


type mobileFixture = {
    driver:WebdriverIO.Browser,
    testData:unknown[]
}


export const test = base.extend<mobileFixture>({
    driver: async ({},use)=>{
        const driver = await mobileDriver()
        try{
            await use(driver)
        }finally{
            await driver.deleteSession?.();
        }
    },
    testData: async ({},use)=>{
        const data:unknown[] = []
        await use(data)
    }
});

export { expect };



