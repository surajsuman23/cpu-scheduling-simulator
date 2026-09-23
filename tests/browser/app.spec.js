import {test,expect} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
test('editable workload, timeline, download and accessibility',async({page})=>{
 await page.goto('/');
 await expect(page.getByRole('status')).toHaveText('Simulation complete');
 await page.getByRole('button',{name:'Round Robin',exact:true}).click();
 await expect(page.getByRole('img',{name:'Round Robin execution timeline'})).toBeVisible();
 await page.getByRole('button',{name:'+ Add process',exact:true}).click();
 await expect(page.locator('#processes tr')).toHaveCount(4);
 await page.getByRole('button',{name:'Run simulation'}).click();
 await expect(page.getByRole('status')).toHaveText('Simulation complete');
 const download=page.waitForEvent('download');await page.getByRole('button',{name:'Download full simulation JSON'}).click();expect((await download).suggestedFilename()).toBe('cpu-simulation.json');
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1)).toBe(true);
 expect((await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21aa']).analyze()).violations).toEqual([]);
 await page.getByLabel('Example workload').selectOption('idle');await expect(page.locator('.legend')).toContainText('IDLE');
 await page.locator('[data-field=id]').nth(1).fill('P1');await page.getByRole('button',{name:'Run simulation'}).click();await expect(page.getByRole('alert')).toBeVisible();
});
