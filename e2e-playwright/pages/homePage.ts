import { ChainablePromiseElement } from 'webdriverio';



export class HomePage {

    constructor(private _driver:WebdriverIO.Browser ){}

     private get title():ChainablePromiseElement {
        return this._driver.$('//android.widget.TextView[@text="Scheduler Assignment Project"]');
    }
    
    private get scrollView():ChainablePromiseElement{
        return this._driver.$('//android.widget.Spinner');
    }


    async getTitleText():Promise<string>{
        return await this.title.getText();
    }

    async clickOnScrollView():Promise<void>{
        await this.scrollView.click();
    }


}