import pandas as pd
import numpy as np
import warnings
from pathlib import Path

# Sopprime warnings pandas
warnings.filterwarnings('ignore')

class AntMetricsAnalyzer:
    def __init__(self, csv_path):
        self.csv_path = csv_path
        self.df = self.load_data()
        
    def print_header(self, title):
        """Stampa un header formattato"""
        print("\n" + "=" * 60)
        print(f" {title.upper()}")
        print("=" * 60)
        
    def print_metric(self, label, value, unit="", details=""):
        """Stampa una metrica formattata"""
        if isinstance(value, float):
            value_str = f"{value:.3f}"
        else:
            value_str = str(value)
        
        print(f"  {label:<30} {value_str} {unit}")
        if details:
            print(f"  {'':<30} ({details})")
    
    def load_data(self):
        """Carica e verifica CSV"""
        try:
            df = pd.read_csv(self.csv_path)
            
            # Preprocessing base
            df['timestamp_sec'] = df['timestamp_ns'] / 1e9
            df['time_relative'] = df['timestamp_sec'] - df['timestamp_sec'].min()
            df['ant_id'] = df['description'].str.extract(r'Ant (\d+)', expand=False)
            df['ant_id'] = pd.to_numeric(df['ant_id'], errors='coerce')
            
            return df
            
        except Exception as e:
            print(f"ERRORE: Impossibile caricare {self.csv_path}")
            print(f"Dettaglio: {e}")
            return pd.DataFrame()
    
    def show_overview(self):
        """Mostra panoramica generale"""
        self.print_header("PANORAMICA SIMULAZIONE")
        
        duration = self.df['time_relative'].max()
        total_events = len(self.df)
        unique_ants = self.df['ant_id'].dropna().nunique()
        
        self.print_metric("File analizzato", Path(self.csv_path).name)
        self.print_metric("Durata simulazione", duration, "secondi")
        self.print_metric("Eventi registrati", f"{total_events:,}")
        self.print_metric("Formiche identificate", unique_ants)
        
        # Distribuzione eventi principali
        event_counts = self.df['event_type'].value_counts()
        main_events = ['ANT_POSITION', 'ANT_DECISION', 'FOOD_DISCOVERED', 'FOOD_PICKUP']
        
        print(f"\n  Distribuzione eventi principali:")
        for event in main_events:
            count = event_counts.get(event, 0)
            pct = (count / total_events * 100) if total_events > 0 else 0
            print(f"    {event:<20} {count:>8,} ({pct:>5.1f}%)")
    
    def analyze_food_metrics(self):
        """Analizza metriche del cibo"""
        self.print_header("ANALISI RACCOLTA CIBO")
        
        food_events = self.df[self.df['event_type'].isin(['FOOD_DISCOVERED', 'FOOD_PICKUP'])]
        
        if food_events.empty:
            print("  Nessun evento di raccolta cibo registrato")
            return {}
        
        # Cibo per formica
        food_per_ant = food_events.groupby('ant_id').size()
        avg_food = food_per_ant.mean()
        
        # Tempo scoperta
        first_discoveries = food_events.groupby('ant_id')['time_relative'].min()
        avg_discovery_time = first_discoveries.mean()
        
        self.print_metric("Formiche che hanno trovato cibo", len(food_per_ant))
        self.print_metric("Media cibo per formica", avg_food, "unita")
        self.print_metric("Tempo medio prima scoperta", avg_discovery_time, "secondi")
        
        # Efficienza nel tempo
        total_duration = self.df['time_relative'].max()
        food_rate = len(food_events) / total_duration if total_duration > 0 else 0
        self.print_metric("Tasso raccolta cibo", food_rate, "unita/sec")
        
        return {
            'ants_found_food': len(food_per_ant),
            'avg_food_per_ant': avg_food,
            'avg_discovery_time': avg_discovery_time,
            'food_rate': food_rate
        }
    
    def analyze_movement_patterns(self):
        """Analizza pattern di movimento"""
        self.print_header("ANALISI MOVIMENTI")
        
        positions = self.df[self.df['event_type'] == 'ANT_POSITION'].copy()
        
        if positions.empty:
            print("  Nessun dato di posizione disponibile")
            return {}
        
        # Analisi stati comportamentali
        states = positions['data'].value_counts()
        total_positions = len(positions)
        
        search_pct = (states.get('SEARCHING', 0) / total_positions * 100) if total_positions > 0 else 0
        return_pct = (states.get('RETURNING', 0) / total_positions * 100) if total_positions > 0 else 0
        
        self.print_metric("Osservazioni movimento", f"{total_positions:,}")
        self.print_metric("Tempo in SEARCHING", search_pct, "%")
        self.print_metric("Tempo in RETURNING", return_pct, "%")
        
        # Calcola velocita media
        speeds = []
        for ant_id in positions['ant_id'].dropna().unique():
            ant_pos = positions[positions['ant_id'] == ant_id].sort_values('time_relative')
            if len(ant_pos) > 1:
                distances = np.sqrt((ant_pos['x'].diff())**2 + (ant_pos['y'].diff())**2)
                time_diffs = ant_pos['time_relative'].diff()
                valid_speeds = (distances / time_diffs).dropna()
                speeds.extend(valid_speeds[valid_speeds < 1000].tolist())  # Filtro outlier
        
        avg_speed = np.mean(speeds) if speeds else 0
        self.print_metric("Velocita media", avg_speed, "unita/sec")
        
        # Area esplorata
        x_range = positions['x'].max() - positions['x'].min()
        y_range = positions['y'].max() - positions['y'].min()
        explored_area = x_range * y_range
        self.print_metric("Area esplorata", f"{explored_area:,.0f}", "unita²")
        
        return {
            'search_percentage': search_pct,
            'return_percentage': return_pct,
            'avg_speed': avg_speed,
            'explored_area': explored_area
        }
    
    def analyze_decisions(self):
        """Analizza decisioni comportamentali"""
        self.print_header("ANALISI DECISIONI")
        
        decisions = self.df[self.df['event_type'] == 'ANT_DECISION']
        
        if decisions.empty:
            print("  Nessuna decisione comportamentale registrata")
            return {}
        
        # Estrai tipi di decisione
        decisions['decision_type'] = decisions['description'].str.extract(r'- (\w+)', expand=False)
        decision_counts = decisions['decision_type'].value_counts()
        
        total_decisions = len(decisions)
        self.print_metric("Decisioni totali registrate", f"{total_decisions:,}")
        
        print(f"\n  Distribuzione tipi di decisione:")
        for decision_type, count in decision_counts.items():
            pct = (count / total_decisions * 100)
            print(f"    {decision_type:<25} {count:>8,} ({pct:>5.1f}%)")
        
        # Analisi uso feromoni
        pheromone_usage = decisions['data'].str.contains('using_pheromones=true', na=False).sum()
        pheromone_pct = (pheromone_usage / total_decisions * 100) if total_decisions > 0 else 0
        
        self.print_metric("Uso feromoni", pheromone_pct, "%", f"{pheromone_usage} su {total_decisions}")
        
        return {
            'total_decisions': total_decisions,
            'pheromone_usage_pct': pheromone_pct,
            'decision_types': decision_counts.to_dict()
        }
    
    def analyze_efficiency(self):
        """Calcola metriche di efficienza"""
        self.print_header("EFFICIENZA COMPLESSIVA")
        
        duration = self.df['time_relative'].max()
        food_events = self.df[self.df['event_type'].isin(['FOOD_DISCOVERED', 'FOOD_PICKUP'])]
        unique_ants = self.df['ant_id'].dropna().nunique()
        
        if duration <= 0:
            print("  Durata insufficiente per calcolare efficienza")
            return {}
        
        # Metriche principali
        food_efficiency = len(food_events) / duration
        ant_productivity = len(food_events) / unique_ants if unique_ants > 0 else 0
        
        self.print_metric("Efficienza raccolta", food_efficiency, "cibo/sec")
        self.print_metric("Produttivita per formica", ant_productivity, "cibo/formica")
        
        # Stima stabilizzazione
        positions = self.df[self.df['event_type'] == 'ANT_POSITION']
        if not positions.empty:
            time_bins = pd.cut(positions['time_relative'], bins=5)
            state_evolution = positions.groupby([time_bins, 'data']).size().unstack(fill_value=0)
            
            if 'RETURNING' in state_evolution.columns and 'SEARCHING' in state_evolution.columns:
                return_ratios = state_evolution['RETURNING'] / (
                    state_evolution['RETURNING'] + state_evolution['SEARCHING']
                )
                stability_reached = (return_ratios > 0.3).any()
                self.print_metric("Stabilizzazione raggiunta", "Si" if stability_reached else "No")
        
        return {
            'food_efficiency': food_efficiency,
            'ant_productivity': ant_productivity
        }
    
    def run_complete_analysis(self):
        """Esegue analisi completa con output pulito"""
        print("\n" + "=" * 60)
        print(" ANALISI SIMULAZIONE FORMICHE - REPORT COMPLETO")
        print("=" * 60)
        
        if self.df.empty:
            print("\nERRORE: File CSV vuoto o non valido")
            return None
        
        # Verifica validita base
        duration = self.df['time_relative'].max()
        if duration <= 0:
            print("\nAVVISO: Simulazione molto breve o timestamps invalidi")
            print("La simulazione potrebbe essere terminata prematuramente")
        
        # Esegui tutte le analisi
        overview = self.show_overview()
        food_metrics = self.analyze_food_metrics()
        movement_metrics = self.analyze_movement_patterns()
        decision_metrics = self.analyze_decisions()
        efficiency_metrics = self.analyze_efficiency()
        
        # Summary finale
        print("\n" + "=" * 60)
        print(" RIASSUNTO ESECUTIVO")
        print("=" * 60)
        
        if food_metrics:
            print(f"  Cibo raccolto: {food_metrics.get('ants_found_food', 0)} formiche attive")
        if movement_metrics:
            print(f"  Comportamento: {movement_metrics.get('search_percentage', 0):.1f}% ricerca, "
                  f"{movement_metrics.get('return_percentage', 0):.1f}% ritorno")
        if decision_metrics:
            print(f"  Decisioni: {decision_metrics.get('pheromone_usage_pct', 0):.1f}% usano feromoni")
        
        return {
            'food': food_metrics,
            'movement': movement_metrics,
            'decisions': decision_metrics,
            'efficiency': efficiency_metrics
        }

# Esecuzione
if __name__ == "__main__":
    import sys
    
    if len(sys.argv) != 2:
        print("Uso: python script.py <file.csv>")
        sys.exit(1)
    
    csv_file = sys.argv[1]
    if not Path(csv_file).exists():
        print(f"Errore: File '{csv_file}' non trovato")
        sys.exit(1)
    
    analyzer = AntMetricsAnalyzer(csv_file)
    results = analyzer.run_complete_analysis()