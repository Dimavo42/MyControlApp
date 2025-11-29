import {test,expect} from './fixtures/mobileFixture';




test.describe("Simple test",async ()=>{

  test("really Simple test",async({driver,testData})=>{
    const title = driver.$('#titleHeader');
    await test.step('expect to have my title as data',async () => {
      expect( await title.getText()).toBe(testData[0])
    })


  })

})
