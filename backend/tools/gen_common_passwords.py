#!/usr/bin/env python3
"""Gera src/main/resources/common-passwords.txt (P-18: >= 10.000 entradas).

A lista combina bases muito comuns (palavras, nomes, padrões de teclado, termos em pt-BR e en) com sufixos
frequentes (sequências numéricas, anos, símbolos). Todas as entradas ficam em minúsculas; a verificação na
aplicação é insensível a maiúsculas/minúsculas. Para produção recomenda-se substituí-la/complementá-la por uma
lista curada (ex.: top senhas vazadas) mantendo o mesmo formato (uma senha por linha).
"""
import itertools, pathlib

bases = """password passw0rd senha senha123 minhasenha admin administrator root qwerty qwertyuiop asdfgh asdfghjkl zxcvbn zxcvbnm
letmein welcome bemvindo iloveyou euteamo amor te-amo teamo love lover monkey dragon master shadow sunshine princess
football futebol flamengo corinthians palmeiras santos saopaulo gremio cruzeiro vasco botafogo fluminense brasil brazil
brasileiro brasileira portugal lisboa saopaulo riodejaneiro minasgerais bahia recife fortaleza curitiba manaus
baseball basketball soccer hockey tennis golf racing mustang ferrari porsche mercedes toyota honda yamaha harley
superman batman spiderman ironman hulk thor joker naruto goku pikachu pokemon mario zelda sonic minecraft fortnite
starwars startrek matrix gandalf hobbit frodo hogwarts harrypotter dumbledore voldemort
freedom whatever trustno1 secret secure security changeme default guest user test testing tester temp temporary
login logout signin signup access open sesame abc abcd abcde abcdef abcdefg abcdefgh abcdefghi
computer internet network server system database windows linux ubuntu android iphone samsung google facebook twitter
instagram youtube netflix amazon apple microsoft oracle github gitlab docker kubernetes
summer winter spring autumn verao inverno primavera outono january february march april may june july august
september october november december janeiro fevereiro marco abril maio junho julho agosto setembro outubro novembro dezembro
monday tuesday wednesday thursday friday saturday sunday segunda terca quarta quinta sexta sabado domingo
michael jennifer jessica ashley michelle daniel andrew joshua matthew david james john robert william richard thomas
charlie jordan hunter tigger buster ginger pepper cookie cheese chocolate banana orange cherry lemon coffee pizza
joao maria jose ana pedro paulo lucas gabriel rafael felipe bruno carlos marcos mateus fernando ricardo eduardo
amanda juliana fernanda patricia camila bianca beatriz larissa leticia mariana gabriela isabela carolina
mamae papai familia amigos amizade felicidade saudade vida deus jesus senhor fe esperanca paz gloria
cachorro gatinho passaro tigre leao cavalo coelho tartaruga borboleta estrela lua sol ceu mar praia
trabalho empresa escola faculdade universidade professor estudante aluno cliente fornecedor vendas financeiro
master123 master1 pass word pass1234 passwd pwd pwd123 p4ssw0rd p@ssw0rd p@ssword passw0rd1
iloveu iloveyou2 ihateyou fuckyou fuckoff asshole bitch bastard shit damn
qazwsx qazwsxedc 1qaz2wsx 1q2w3e4r 1q2w3e4r5t 1qazxsw2 zaq12wsx q1w2e3r4 q1w2e3r4t5 qweasd qweasdzxc
asdasd asdqwe zxcasd poiuytrewq lkjhgfdsa mnbvcxz mnbvcxz1 poiuyt lkjhgf
trustno1 starwars1 dragon123 monkey123 shadow123 master123 letmein123 welcome123 admin123 root123 user123 guest123
abc123 abc1234 a1b2c3 a1b2c3d4 123abc 1a2b3c 1a2b3c4d aaaaaa bbbbbb cccccc dddddd eeeeee ffffff gggggg hhhhhh
""".split()

suffixes = ["", "1", "2", "3", "7", "12", "13", "21", "22", "23", "69", "77", "88", "99", "00", "01", "10", "11",
            "123", "321", "456", "789", "007", "666", "777", "888", "999", "000", "111", "222", "333", "444", "555",
            "1234", "4321", "1111", "2222", "3333", "0000", "12345", "54321", "123456", "654321", "1234567", "12345678",
            "123456789", "1234567890", "!", "!!", "@", "#", "$", "*", ".", "_", "-", "1!", "123!", "@123", "#123",
            "@2020", "@2021", "@2022", "@2023", "@2024", "@2025", "@2026",
            "2000", "2001", "2002", "2003", "2004", "2005", "2006", "2007", "2008", "2009", "2010", "2011", "2012",
            "2013", "2014", "2015", "2016", "2017", "2018", "2019", "2020", "2021", "2022", "2023", "2024", "2025",
            "2026", "1990", "1991", "1992", "1993", "1994", "1995", "1996", "1997", "1998", "1999", "1980", "1985"]

prefixes = [""]

out = set()
for b in bases:
    for p in prefixes:
        for s in suffixes:
            out.add((p + b + s).lower())

# Sequências e repetições
for n in range(6, 41):
    out.add("1" * n); out.add("0" * n); out.add("a" * n)
digits = "01234567890123456789012345678901234567890"
for n in range(6, 41):
    out.add(digits[1:n + 1]); out.add(digits[:n]); out.add(digits[:n][::-1])
rows = ["qwertyuiop", "asdfghjkl", "zxcvbnm", "1234567890", "qazwsxedcrfv", "abcdefghijklmnopqrstuvwxyz"]
for r in rows:
    for n in range(6, len(r) + 1):
        out.add(r[:n]); out.add(r[:n][::-1])
        out.add(r[:n] + "1"); out.add(r[:n] + "123")
# Datas dd/mm/aaaa em texto (ddmmaaaa) para anos recentes e mm/dd mais comuns
for d, m in itertools.product(range(1, 32), range(1, 13)):
    for y in (1980, 1985, 1990, 1995, 2000, 2005):
        out.add("%02d%02d%d" % (d, m, y))

lines = sorted(x for x in out if 8 <= len(x) <= 40)
path = pathlib.Path(__file__).resolve().parents[1] / "src/main/resources/common-passwords.txt"
path.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(len(lines), "entradas ->", path)
