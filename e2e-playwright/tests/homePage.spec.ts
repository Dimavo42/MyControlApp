import {test,expect} from './fixtures/mobileFixture';
import { HomePage } from '../pagesInternals';



test.describe("Simple test",async ()=>{

  test("really Simple test",async({driver})=>{
    const homePage = new HomePage(driver);
    await test.step('expect title to be',async () => {
      const localTest = await homePage.getTitleText() ;
      expect(await homePage.getTitleText()).toEqual("Scheduler Assignment Project");
    })


  })

})
