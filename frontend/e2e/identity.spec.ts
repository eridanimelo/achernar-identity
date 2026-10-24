import { test, expect, APIRequestContext, Page } from '@playwright/test';
import { readFileSync } from 'node:fs';
const env=Object.fromEntries(readFileSync('../docker/.env','utf8').trim().split('\n').map(line=>{const i=line.indexOf('=');return [line.slice(0,i),line.slice(i+1)];}));
const password='SenhaSeguraParaTeste123!';
async function emailLink(request:APIRequestContext,email:string,path:string){
 let result='';
 await expect.poll(async()=>{const list=await (await request.get('http://localhost:8025/api/v1/messages')).json();
 for(const mail of list.messages??[]){if((mail.To??[]).some((to:{Address:string})=>to.Address===email)){
 const detail=await (await request.get(`http://localhost:8025/api/v1/message/${mail.ID}`)).json();
 const link=(detail.Text as string).match(/http:\/\/localhost:4200\/auth\/[^\s]+/g)?.find(item=>item.includes(path));if(link){result=link;return true;}
 }}return false;},{timeout:20000}).toBe(true);return result;
}
async function login(page:Page,email:string,pass:string){await page.goto('/auth/login');await page.getByLabel('E-mail',{exact:true}).fill(email);await page.getByLabel('Senha',{exact:true}).fill(pass);await page.getByRole('button',{name:'Entrar na minha conta'}).click();await expect(page).toHaveURL(/\/account$/);}
test('cadastro, e-mail, recuperação e administração em navegador',async({page,request,browser})=>{
 const errors:string[]=[];page.on('pageerror',error=>errors.push(error.message));const email=`browser-${Date.now()}@example.com`;
 await page.goto('/auth/register');await page.getByLabel('Nome',{exact:true}).fill('Pessoa Navegador');await page.getByLabel('E-mail',{exact:true}).fill(email);await page.getByLabel('Senha',{exact:true}).fill(password);await page.getByRole('button',{name:'Criar conta',exact:true}).click();await expect(page.getByRole('status')).toContainText('receberá um e-mail');
 await page.goto(await emailLink(request,email,'verify'));await page.getByRole('button',{name:'Confirmar meu e-mail'}).click();await expect(page.getByRole('status')).toContainText('E-mail confirmado');
 await login(page,email,password);await page.reload();await expect(page.getByRole('heading',{name:'Minha conta'})).toBeVisible();
 await page.getByLabel('Alterar nome').fill('Pessoa Atualizada');await page.getByRole('button',{name:'Salvar nome'}).click();await expect(page.getByRole('status')).toContainText('Nome atualizado');
 await page.getByRole('button',{name:'Sair de todas as sessões'}).click();await expect(page).toHaveURL(/\/auth\/login$/);
 await page.goto('/auth/forgot-password');await page.getByLabel('E-mail',{exact:true}).fill(email);await page.getByRole('button',{name:'Enviar instruções'}).click();await expect(page.getByRole('status')).toBeVisible();
 await page.goto(await emailLink(request,email,'reset-password'));await page.getByLabel('Senha',{exact:true}).fill('NovaSenhaParaNavegador123!');await page.getByRole('button',{name:'Salvar nova senha'}).click();await expect(page.getByRole('status')).toContainText('Senha redefinida');await login(page,email,'NovaSenhaParaNavegador123!');
 const context=await browser.newContext();const admin=await context.newPage();await login(admin,env.ADMIN_EMAIL,env.ADMIN_PASSWORD);await admin.getByRole('link',{name:'Usuários',exact:true}).click();
 await admin.getByLabel('Buscar por nome ou e-mail').fill(email);await admin.getByRole('button',{name:'Buscar',exact:true}).click();const card=admin.locator('article').filter({hasText:email});await expect(card).toHaveCount(1);
 await card.getByRole('button',{name:'Bloquear',exact:true}).click();await expect(card.getByText('Bloqueado',{exact:true})).toBeVisible();await page.reload();await expect(page).toHaveURL(/\/auth\/login$/);
 await card.getByRole('button',{name:'Desbloquear',exact:true}).click();await expect(card.getByText('Ativo',{exact:true})).toBeVisible();await card.getByRole('button',{name:'Excluir',exact:true}).click();await expect(admin.getByRole('alertdialog')).toBeVisible();await admin.getByRole('button',{name:'Cancelar',exact:true}).click();await expect(card).toHaveCount(1);
 await card.getByRole('button',{name:'Excluir',exact:true}).click();await admin.getByRole('button',{name:'Confirmar exclusão'}).click();await expect(admin.getByRole('heading',{name:'Nenhum usuário encontrado'})).toBeVisible();expect(errors).toEqual([]);await context.close();
});
test('layout móvel e validação de formulário',async({page})=>{
 await page.setViewportSize({width:390,height:844});await page.goto('/auth/register');await expect(page.getByRole('button',{name:'Criar conta',exact:true})).toBeDisabled();
 await page.getByLabel('E-mail',{exact:true}).fill('invalido');await page.getByLabel('Nome',{exact:true}).click();await expect(page.getByText('Informe um e-mail válido.')).toBeVisible();
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);await page.screenshot({path:'test-results/mobile-register.png',fullPage:true});
 await page.setViewportSize({width:1440,height:1000});await page.goto('/auth/login');await page.screenshot({path:'test-results/desktop-login.png',fullPage:true});
});
