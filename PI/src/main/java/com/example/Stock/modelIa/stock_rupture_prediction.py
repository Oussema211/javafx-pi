import pandas as pd
import numpy as np
from sklearn.preprocessing import LabelEncoder, StandardScaler
from xgboost import XGBRegressor
from prophet import Prophet
from datetime import datetime, timedelta
import uuid
import json
import matplotlib.pyplot as plt

# Charger les données réelles à partir des classes Java OrderSummary et Stock
def charger_donnees(order_summaries, stocks):
    # Préparer les listes pour construire le DataFrame
    data = {
        'produit_id': [],
        'entrepot_id': [],
        'quantite_actuelle': [],
        'prix_unitaire': [],
        'category': [],
        'espace': [],
        'latitude': [],
        'longitude': [],
        'ville': [],
        'date_entree': [],
        'date_sortie': [],
        'quantite_sortie': [],
        'date_achat': []
    }

    # Dictionnaire pour suivre les stocks par produit_id
    stock_dict = {str(stock.getProduitId()): stock for stock in stocks}

    # Traiter chaque OrderSummary
    for order in order_summaries:
        for produit_commande in order.getProduitsCommandes():
            produit_id = str(produit_commande.getProduit().getId())
            stock = stock_dict.get(produit_id)

            # Vérifier si le stock existe pour ce produit
            if stock is None:
                continue  # Ignorer si aucun stock correspondant

            # Extraire les entrepôts (supposons un seul entrepôt pour simplifier)
            entrepot_id = str(list(stock.getEntrepotIds())[0]) if stock.getEntrepotIds() else str(uuid.uuid4())

            # Remplir les données
            data['produit_id'].append(produit_id)
            data['entrepot_id'].append(entrepot_id)
            data['quantite_actuelle'].append(stock.getSeuilAlert() or np.random.randint(0, 100))  # Quantité actuelle approximative
            data['prix_unitaire'].append(produit_commande.getPrixUnitaire() or np.random.uniform(10, 1000))
            data['category'].append(produit_commande.getProduit().getCategorie() or np.random.choice(['Semences', 'Engrais', 'Fruits']))
            data['espace'].append(np.random.uniform(100, 10000))  # Non fourni, simulé
            data['latitude'].append(np.random.uniform(-90, 90))  # Non fourni, simulé
            data['longitude'].append(np.random.uniform(-180, 180))  # Non fourni, simulé
            data['ville'].append(np.random.choice(['Paris', 'Lyon', 'Marseille']))  # Non fourni, simulé
            data['date_entree'].append(stock.getDateEntree() or datetime.now() - timedelta(days=np.random.randint(1, 365)))
            data['date_sortie'].append(stock.getDateSortie() or (stock.getDateEntree() + timedelta(days=np.random.randint(1, 30))))
            data['quantite_sortie'].append(produit_commande.getQuantite())
            data['date_achat'].append(datetime.fromisoformat(order.getDateAchat().replace('Z', '+00:00')))

    # Convertir en DataFrame
    df = pd.DataFrame(data)

    # Convertir les dates en datetime si nécessaire
    df['date_entree'] = pd.to_datetime(df['date_entree'])
    df['date_sortie'] = pd.to_datetime(df['date_sortie'])
    df['date_achat'] = pd.to_datetime(df['date_achat'])

    return df

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

# Graphique à partir de rapport_stock.json
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
    plt.savefig('ruptures_plot.png')  # Sauvegarder au lieu de plt.show()

# Fonction principale
def main(order_summaries, stocks):
    df = charger_donnees(order_summaries, stocks)
    df = pretraiter_donnees(df)
    df, modele = predire_rupture(df)
    rapport = generer_rapport(df)
    print("Rapport généré :", rapport)
    generer_graphique_ruptures()

# Exemple d'utilisation
if __name__ == "__main__":
    # Simuler des données Java (remplacer par vos vraies données)
    from datetime import datetime
    import uuid

    class Produit:
        def __init__(self, id, categorie):
            self.id = id
            self.categorie = categorie
        def getId(self):
            return self.id
        def getCategorie(self):
            return self.categorie

    class ProduitCommande:
        def __init__(self, produit, quantite, prix_unitaire):
            self.produit = produit
            self.quantite = quantite
            self.prix_unitaire = prix_unitaire
        def getProduit(self):
            return self.produit
        def getQuantite(self):
            return self.quantite
        def getPrixUnitaire(self):
            return self.prix_unitaire

    class OrderSummary:
        def __init__(self, id, userId, dateAchat, prixTotal, produitsCommandes):
            self.id = id
            self.userId = userId
            self.dateAchat = dateAchat
            self.prixTotal = prixTotal
            self.produitsCommandes = produitsCommandes
        def getId(self):
            return self.id
        def getUserId(self):
            return self.userId
        def getDateAchat(self):
            return self.dateAchat
        def getPrixTotal(self):
            return self.prixTotal
        def getProduitsCommandes(self):
            return self.produitsCommandes

    class Stock:
        def __init__(self, id, produitId, dateEntree, seuilAlert, userId, entrepotIds):
            self.id = id
            self.produitId = produitId
            self.dateEntree = dateEntree
            self.seuilAlert = seuilAlert
            self.userId = userId
            self.entrepotIds = entrepotIds
        def getId(self):
            return self.id
        def getProduitId(self):
            return self.produitId
        def getDateEntree(self):
            return self.dateEntree
        def getDateSortie(self):
            return self.dateEntree + timedelta(days=np.random.randint(1, 30))
        def getSeuilAlert(self):
            return self.seuilAlert
        def getUserId(self):
            return self.userId
        def getEntrepotIds(self):
            return self.entrepotIds

    # Simuler des données
    produits = [Produit(str(uuid.uuid4()), 'Semences') for _ in range(5)]
    produits_commandes = [ProduitCommande(p, np.random.randint(1, 50), np.random.uniform(10, 1000)) for p in produits]
    order_summaries = [
        OrderSummary(str(uuid.uuid4()), str(uuid.uuid4()), datetime.now().isoformat(), 1000.0, produits_commandes[:2]),
        OrderSummary(str(uuid.uuid4()), str(uuid.uuid4()), (datetime.now() - timedelta(days=30)).isoformat(), 1500.0, produits_commandes[2:])
    ]
    stocks = [
        Stock(str(uuid.uuid4()), p.getId(), datetime.now() - timedelta(days=60), np.random.randint(10, 100), str(uuid.uuid4()), {str(uuid.uuid4())})
        for p in produits
    ]

    main(order_summaries, stocks)