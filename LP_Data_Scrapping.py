#!/usr/bin/env python3
import argparse, time
import pandas as pd
from bs4 import BeautifulSoup, Comment
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from webdriver_manager.chrome import ChromeDriverManager

DESIRED = ['Player','Nation','Squad','Pos','Age','MP','Starts','Min','Gls','Ast','PK','CrdY','CrdR','Expected_xG','xA']


def selenium_table(url, headless=True):
    opts = Options()
    if headless: opts.add_argument('--headless=new')
    opts.add_argument('user-agent=Mozilla/5.0')
    driver = webdriver.Chrome(service=Service(ChromeDriverManager().install()), options=opts)
    try:
        try:
            driver.execute_cdp_cmd('Page.addScriptToEvaluateOnNewDocument', {'source':"Object.defineProperty(navigator,'webdriver',{get:()=>false});"})
        except Exception:
            pass
        driver.get(url); time.sleep(1.2)
        try:
            el = driver.find_element(By.ID, 'stats_standard')
            return pd.read_html(el.get_attribute('outerHTML'))[0]
        except Exception:
            s = BeautifulSoup(driver.page_source, 'lxml')
            for c in s.find_all(string=lambda t: isinstance(t, Comment)):
                if '<table' in c and ('stats_standard' in c or 'stats_table' in c):
                    return pd.read_html(str(c))[0]
    finally:
        try: driver.quit()
        except: pass
    return None


def flatten(df):
    df.columns = ['_'.join([str(p).strip() for p in col if str(p).strip() and 'unnamed' not in str(p).lower()])
                  if isinstance(col, tuple) else str(col) for col in df.columns]
    # strip unwanted top-level prefixes (e.g. Performance_Gls -> Gls)
    cleaned = []
    for c in df.columns:
        c2 = str(c)
        for pref in ('Performance_', 'Perfromace_'):
            if c2.startswith(pref):
                c2 = c2[len(pref):]
                break
        cleaned.append(c2)
    df.columns = cleaned
    m = {k:'MP' if 'MP' in k and 'Playing' in k else 'Starts' if 'Starts' in k and 'Playing' in k else 'Min' if 'Min' in k and 'Playing' in k else None
         for k in df.columns}
    rename = {k:v for k,v in m.items() if v}
    if rename: df = df.rename(columns=rename)
    return df


def pick(df, want):
    cols = list(df.columns); out=[]
    for w in want:
        lw=w.lower(); f=next((c for c in cols if c.lower()==lw), None)
        if not f: f=next((c for c in cols if lw in c.lower()), None)
        if f and f not in out: out.append(f)
    return df[out] if out else df


def normalize_age(df):
    if 'Age' in df.columns:
        df['Age']=pd.to_numeric(df['Age'].astype(str).str.split('-').str[0], errors='coerce').astype('Int64')
    return df


def main():
    p=argparse.ArgumentParser(); p.add_argument('--out',default='players_primeira_liga.csv'); p.add_argument('--headless',action='store_true')
    args=p.parse_args(); url='https://fbref.com/en/comps/32/stats/Primeira-Liga-Stats'
    df = selenium_table(url, headless=args.headless)
    if df is None: raise SystemExit('No table')
    # clean possible repeated header rows that appear in the table (fbref repeats headers)
    # normalize Player column early so we can reliably filter rows like 'Player' or empty values
    if 'Player' in df.columns:
        df['Player'] = df['Player'].astype(str).str.strip()
        df = df[~df['Player'].str.lower().eq('player')]
    df=df.dropna(axis=1, how='all')
    df=flatten(df)
    out=pick(df, DESIRED)
    out=normalize_age(out)
    # final safety: remove any leftover header-like rows or empty player names after column picking
    if 'Player' in out.columns:
        out['Player'] = out['Player'].astype(str).str.strip()
        out = out[~out['Player'].str.lower().eq('player')]
        out = out[out['Player'].astype(bool)]
    out.to_csv(args.out, index=False, encoding='utf-8-sig')
    print('Wrote', args.out)


if __name__=='__main__':
    main()