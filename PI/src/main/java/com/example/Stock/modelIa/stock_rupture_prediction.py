import pandas as pd
import numpy as np
from sklearn.preprocessing import LabelEncoder, StandardScaler
from xgboost import XGBRegressor
from prophet import Prophet
from datetime import datetime, timedelta
import uuid
import json
import matplotlib.pyplot as plt

# Charger les données simulées
def charger_donnees():
    n = 20
    produits = [str(uuid.uuid4()) for _ in range(n)]
    entrepots = [str(uuid.uuid4()) for _ in range(n)]
    dates = [datetime.now() - timedelta(days=np.random.randint(1, 365)) for _ in range(n)]
    data = {
        'produit_id': produits,
        'entrepot_id': entrepots,
        'quantite_actuelle': np.random.randint(0, 100, n),
        'prix_unitaire': np.random.uniform(10, 1000, n),
        'category': np.random.choice(['Semences', 'Engrais', 'Fruits'], n),
        'espace': np.random.uniform(100, 10000, n),
        'latitude': np.random.uniform(-90, 90, n),
        'longitude': np.random.uniform(-180, 180, n),
        'ville': np.random.choice(['Paris', 'Lyon', 'Marseille'], n),
        'date_entree': dates,
        'date_sortie': [d + timedelta(days=np.random.randint(1, 30)) for d in dates],
        'quantite_sortie': np.random.randint(1, 50, n),
        'date_achat': dates,
    }
    return pd.DataFrame(data)

# Prétraitement
def pretraiter_donnees(df):
    df['jours_consommation'] = (df['date_sortie'] - df['date_entree']).dt.days
    df['vitesse_consommation'] = df['quantite_sortie'] / df['jours_consommation'].replace(0, 1)
    df['mois'] = df['date_achat'].dt.month
    df['saison'] = df['mois'].map({1: 'Hiver', 2: 'Hiver', 3: 'Printemps', 4: 'Printemps',
                                   5: 'Printemps', 6: 'Été', 7: 'Été', 8: 'Été',
                                   9: 'Automne', 10: 'Automne', 11: 'Automne', 12: 'Hiver'})
    encodeur = LabelEncoder()
    df['category'] = encodeur.fit_transform(df['category'])
    df['ville'] = encodeur.fit_transform(df['ville'])
    df['saison'] = encodeur.fit_transform(df['saison'])
    colonnes_numeriques = ['quantite_actuelle', 'prix_unitaire', 'espace', 'latitude',
                           'longitude', 'vitesse_consommation']
    normaliseur = StandardScaler()
    df[colonnes_numeriques] = normaliseur.fit_transform(df[colonnes_numeriques])
    return df

# Prédiction
def predire_rupture(df):
    caracteristiques = ['quantite_actuelle', 'prix_unitaire', 'category', 'espace',
                        'latitude', 'longitude', 'ville', 'vitesse_consommation', 'mois', 'saison']
    X = df[caracteristiques]
    y = df['quantite_actuelle'] / df['vitesse_consommation'].replace(0, 1)
    modele = XGBRegressor(n_estimators=100, random_state=42)
    modele.fit(X, y)
    predictions = modele.predict(X)
    df['jours_avant_rupture'] = predictions
    return df, modele

# Saisonnalité avec Prophet
def analyser_saisonnalite(df, produit_id):
    df_produit = df[df['produit_id'] == produit_id][['date_achat', 'quantite_sortie']]
    df_produit = df_produit.groupby('date_achat').sum().reset_index()
    df_produit.columns = ['ds', 'y']
    if df_produit.shape[0] < 2 or df_produit['y'].isna().all():
        return 0
    modele_prophet = Prophet(yearly_seasonality=True, weekly_seasonality=False, daily_seasonality=False)
    modele_prophet.fit(df_produit)
    futur = modele_prophet.make_future_dataframe(periods=365)
    prevision = modele_prophet.predict(futur)
    mois_prochain = (datetime.now() + timedelta(days=30)).month
    prevision_mois = prevision[prevision['ds'].dt.month == mois_prochain]
    quantite_recommandee = prevision_mois['yhat'].mean()
    return quantite_recommandee

# Produit le plus demandé
def produit_plus_demande(df):
    demande_par_produit = df.groupby('produit_id')['quantite_sortie'].sum().reset_index()
    produit_id = demande_par_produit.loc[demande_par_produit['quantite_sortie'].idxmax(), 'produit_id']
    quantite_totale = demande_par_produit['quantite_sortie'].max()
    return {"produit_id": produit_id, "quantite_totale": float(quantite_totale)}

# Rapport JSON
def generer_rapport(df, seuil_rupture=7):
    rapport = {
        "alertes": [],
        "recommandations_saison": [],
        "produit_plus_demande": produit_plus_demande(df),
        "total_produits": len(df['produit_id'].unique()),
        "total_entrepots": len(df['entrepot_id'].unique()),
        "produits_stock_faible": 0
    }
    deja_traite = set()
    for _, row in df.iterrows():
        if row['jours_avant_rupture'] < seuil_rupture:
            alerte = {
                "produit_id": row['produit_id'],
                "entrepot_id": row['entrepot_id'],
                "jours_avant_rupture": row['jours_avant_rupture'],
                "quantite_actuelle": row['quantite_actuelle']
            }
            rapport["alertes"].append(alerte)
            rapport["produits_stock_faible"] += 1
        if row['produit_id'] not in deja_traite:
            quantite_recommandee = analyser_saisonnalite(df, row['produit_id'])
            recommandation = {
                "produit_id": row['produit_id'],
                "mois_prochain": (datetime.now() + timedelta(days=30)).strftime("%B %Y"),
                "quantite_recommandee": quantite_recommandee
            }
            rapport["recommandations_saison"].append(recommandation)
            deja_traite.add(row['produit_id'])

    with open('rapport_stock.json', 'w') as f:
        json.dump(rapport, f, indent=4)
    return rapport

# ✅ Graphique à partir de rapport_stock.json
def generer_graphique_ruptures(fichier_rapport='rapport_stock.json'):
    with open(fichier_rapport, 'r') as f:
        data = json.load(f)
    alertes = data.get("alertes", [])
    if not alertes:
        print("Aucune alerte à afficher.")
        return
    labels = [f"{a['produit_id'][:4]}..@{a['entrepot_id'][:4]}.." for a in alertes]
    jours = [a['jours_avant_rupture'] for a in alertes]
    plt.figure(figsize=(12, 6))
    bars = plt.bar(labels, jours, color='tomato')
    plt.title("Jours avant rupture - Produits en alerte")
    plt.ylabel("Jours avant rupture")
    plt.xticks(rotation=45, ha='right')
    for bar in bars:
        yval = round(bar.get_height(), 1)
        plt.text(bar.get_x() + bar.get_width()/2, yval + 0.1, f"{yval}", ha='center', va='bottom', fontsize=8)
    plt.tight_layout()
    plt.grid(axis='y', linestyle='--', alpha=0.5)
    plt.show()

# Fonction principale
def main():
    df = charger_donnees()
    df = pretraiter_donnees(df)
    df, modele = predire_rupture(df)
    rapport = generer_rapport(df)
    print("Rapport généré :", rapport)
    generer_graphique_ruptures()

if __name__ == "__main__":
    main()
