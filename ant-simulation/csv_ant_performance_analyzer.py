#!/usr/bin/env python3
"""
Script per analisi delle performance delle formiche con calcolo preciso delle distanze
Crea grafici di declino temporale di velocità ed efficienza
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import argparse
from pathlib import Path
import warnings
from tqdm import tqdm
import time
from typing import Dict, List, Tuple

warnings.filterwarnings('ignore')

class AntPerformanceAnalyzer:
    def __init__(self, csv_file_path: str):
        """
        Inizializza l'analizzatore con il percorso del file CSV
        """
        self.csv_path = Path(csv_file_path)
        self.df = None
        self.trips_data = []
        self.performance_metrics = []
        
    def load_and_filter_csv(self):
        """
        Carica il CSV e filtra solo gli eventi rilevanti per ottimizzare la performance
        """
        print(f"Caricamento CSV da: {self.csv_path}")
        start_time = time.time()
        
        # Carica il CSV
        self.df = pd.read_csv(self.csv_path)
        print(f"CSV caricato: {len(self.df)} righe in {time.time() - start_time:.1f}s")
        
        # Filtra solo eventi rilevanti
        relevant_events = ['FOOD_PICKUP', 'FOOD_DROP', 'ANT_POSITION']
        self.df = self.df[self.df['event_type'].isin(relevant_events)].copy()
        
        # Converti timestamp in numerico e ordina
        self.df['timestamp_ns'] = pd.to_numeric(self.df['timestamp_ns'])
        self.df = self.df.sort_values('timestamp_ns').reset_index(drop=True)
        
        print(f"Eventi filtrati: {len(self.df)} righe ({relevant_events})")
        
        # Estrai ID formica dai description per eventi PICKUP/DROP
        def extract_ant_id(row):
            if row['event_type'] in ['FOOD_PICKUP', 'FOOD_DROP']:
                desc = str(row['description'])
                if 'Ant ' in desc:
                    try:
                        ant_id = int(desc.split('Ant ')[1].split(' ')[0])
                        return ant_id
                    except:
                        return None
            elif row['event_type'] == 'ANT_POSITION':
                desc = str(row['description'])
                if 'Ant ' in desc:
                    try:
                        ant_id = int(desc.split('Ant ')[1].split(',')[0])
                        return ant_id
                    except:
                        return None
            return None
        
        print("Estrazione ID formiche...")
        self.df['ant_id'] = self.df.apply(extract_ant_id, axis=1)
        
        # Rimuovi righe senza ant_id valido
        initial_count = len(self.df)
        self.df = self.df.dropna(subset=['ant_id']).copy()
        self.df['ant_id'] = self.df['ant_id'].astype(int)
        
        print(f"Righe con ant_id valido: {len(self.df)} (rimosse {initial_count - len(self.df)})")
        print(f"Formiche uniche trovate: {self.df['ant_id'].nunique()}")
        
    def extract_trips(self):
        """
        Estrae i viaggi (pickup -> drop) per ogni formica
        """
        print("Estrazione viaggi pickup->drop...")
        
        pickup_events = self.df[self.df['event_type'] == 'FOOD_PICKUP'].copy()
        drop_events = self.df[self.df['event_type'] == 'FOOD_DROP'].copy()
        
        print(f"Eventi PICKUP: {len(pickup_events)}")
        print(f"Eventi DROP: {len(drop_events)}")
        
        trips = []
        
        # Per ogni formica, matcha pickup con drop successivi
        for ant_id in tqdm(pickup_events['ant_id'].unique(), desc="Processing ants"):
            ant_pickups = pickup_events[pickup_events['ant_id'] == ant_id].sort_values('timestamp_ns')
            ant_drops = drop_events[drop_events['ant_id'] == ant_id].sort_values('timestamp_ns')
            
            # Matcha ogni pickup con il primo drop successivo
            drop_index = 0
            for _, pickup in ant_pickups.iterrows():
                # Trova il prossimo drop dopo questo pickup
                while drop_index < len(ant_drops):
                    drop = ant_drops.iloc[drop_index]
                    if drop['timestamp_ns'] > pickup['timestamp_ns']:
                        # Trovato drop corrispondente
                        trip = {
                            'ant_id': ant_id,
                            'pickup_time': pickup['timestamp_ns'],
                            'drop_time': drop['timestamp_ns'],
                            'pickup_x': pickup['x'],
                            'pickup_y': pickup['y'],
                            'drop_x': drop['x'],
                            'drop_y': drop['y'],
                            'duration': (drop['timestamp_ns'] - pickup['timestamp_ns']) / 1e9  # in secondi
                        }
                        trips.append(trip)
                        drop_index += 1
                        break
                    drop_index += 1
        
        self.trips_data = trips
        print(f"Viaggi estratti: {len(self.trips_data)}")
        
        # Statistiche di base
        if self.trips_data:
            durations = [trip['duration'] for trip in self.trips_data]
            print(f"Durata viaggi - Min: {min(durations):.2f}s, Max: {max(durations):.2f}s, Media: {np.mean(durations):.2f}s")
    
    def calculate_precise_distances(self):
        """
        Calcola le distanze precise per ogni viaggio usando le posizioni ANT_POSITION
        """
        print("Calcolo distanze precise dei percorsi...")
        
        # Filtra solo posizioni ANT_POSITION per performance
        position_data = self.df[self.df['event_type'] == 'ANT_POSITION'].copy()
        print(f"Posizioni disponibili: {len(position_data)}")
        
        enhanced_trips = []
        
        for trip in tqdm(self.trips_data, desc="Calculating distances"):
            ant_id = trip['ant_id']
            start_time = trip['pickup_time']
            end_time = trip['drop_time']
            
            # Filtra posizioni per questa formica in questo intervallo temporale
            ant_positions = position_data[
                (position_data['ant_id'] == ant_id) &
                (position_data['timestamp_ns'] >= start_time) &
                (position_data['timestamp_ns'] <= end_time)
            ].sort_values('timestamp_ns')
            
            if len(ant_positions) < 2:
                # Se non ci sono abbastanza posizioni, usa distanza euclidea diretta
                direct_distance = np.sqrt(
                    (trip['drop_x'] - trip['pickup_x'])**2 + 
                    (trip['drop_y'] - trip['pickup_y'])**2
                )
                total_distance = direct_distance
            else:
                # Calcola distanza sommando segmenti tra posizioni consecutive
                positions = ant_positions[['x', 'y']].values
                
                # Aggiungi punto di pickup all'inizio se non c'è già
                first_pos = positions[0]
                pickup_pos = np.array([trip['pickup_x'], trip['pickup_y']])
                if np.linalg.norm(first_pos - pickup_pos) > 10:  # Se distanza > 10 pixel
                    positions = np.vstack([pickup_pos, positions])
                
                # Aggiungi punto di drop alla fine se non c'è già
                last_pos = positions[-1]
                drop_pos = np.array([trip['drop_x'], trip['drop_y']])
                if np.linalg.norm(last_pos - drop_pos) > 10:  # Se distanza > 10 pixel
                    positions = np.vstack([positions, drop_pos])
                
                # Calcola distanze tra punti consecutivi
                distances = np.sqrt(np.sum(np.diff(positions, axis=0)**2, axis=1))
                total_distance = np.sum(distances)
            
            # Calcola metriche di performance
            velocity = total_distance / trip['duration'] if trip['duration'] > 0 else 0
            efficiency = total_distance / (trip['duration']**1.5) if trip['duration'] > 0 else 0
            
            enhanced_trip = trip.copy()
            enhanced_trip.update({
                'distance': total_distance,
                'velocity': velocity,
                'efficiency': efficiency,
                'num_positions': len(ant_positions)
            })
            
            enhanced_trips.append(enhanced_trip)
        
        self.trips_data = enhanced_trips
        print(f"Distanze calcolate per {len(self.trips_data)} viaggi")
        
        # Statistiche distanze
        distances = [trip['distance'] for trip in self.trips_data]
        velocities = [trip['velocity'] for trip in self.trips_data]
        efficiencies = [trip['efficiency'] for trip in self.trips_data]
        
        print(f"Distanze - Min: {min(distances):.1f}, Max: {max(distances):.1f}, Media: {np.mean(distances):.1f}")
        print(f"Velocità - Min: {min(velocities):.1f}, Max: {max(velocities):.1f}, Media: {np.mean(velocities):.1f}")
        print(f"Efficienza - Min: {min(efficiencies):.1f}, Max: {max(efficiencies):.1f}, Media: {np.mean(efficiencies):.1f}")
    
    def prepare_temporal_analysis(self):
        """
        Prepara i dati per l'analisi temporale
        """
        print("Preparazione analisi temporale...")
        
        if not self.trips_data:
            print("Errore: Nessun viaggio disponibile")
            return
        
        # Converti in DataFrame per facilità di manipolazione
        df_trips = pd.DataFrame(self.trips_data)
        
        # Ordina per tempo di pickup
        df_trips = df_trips.sort_values('pickup_time').reset_index(drop=True)
        
        # Calcola tempo percentuale (0-100%)
        min_time = df_trips['pickup_time'].min()
        max_time = df_trips['pickup_time'].max()
        time_span = max_time - min_time
        
        df_trips['time_percent'] = ((df_trips['pickup_time'] - min_time) / time_span) * 100
        
        self.performance_metrics = df_trips
        
        # Statistiche prima/seconda metà
        first_half = df_trips[df_trips['time_percent'] <= 50]
        second_half = df_trips[df_trips['time_percent'] > 50]
        
        if len(first_half) > 0 and len(second_half) > 0:
            vel_first = first_half['velocity'].mean()
            vel_second = second_half['velocity'].mean()
            vel_decline = ((vel_second - vel_first) / vel_first) * 100
            
            eff_first = first_half['efficiency'].mean()
            eff_second = second_half['efficiency'].mean()
            eff_decline = ((eff_second - eff_first) / eff_first) * 100
            
            print(f"\nAnalisi temporale:")
            print(f"Prima metà - Velocità media: {vel_first:.2f}, Efficienza media: {eff_first:.2f}")
            print(f"Seconda metà - Velocità media: {vel_second:.2f}, Efficienza media: {eff_second:.2f}")
            print(f"Declino velocità: {vel_decline:.2f}%")
            print(f"Declino efficienza: {eff_decline:.2f}%")
            
            self.velocity_decline = vel_decline
            self.efficiency_decline = eff_decline
        else:
            print("Attenzione: Non ci sono abbastanza dati per dividere in prima/seconda metà")
            self.velocity_decline = 0
            self.efficiency_decline = 0
    
    def calculate_moving_averages(self, window=50):
        """
        Calcola medie mobili per i trend
        """
        df = self.performance_metrics.copy()
        
        # Media mobile globale
        df['velocity_ma'] = df['velocity'].rolling(window=window, center=True, min_periods=1).mean()
        df['efficiency_ma'] = df['efficiency'].rolling(window=window, center=True, min_periods=1).mean()
        
        # Media mobile per formica (trend medio)
        ant_ma_data = []
        for ant_id in df['ant_id'].unique():
            ant_data = df[df['ant_id'] == ant_id].sort_values('pickup_time')
            if len(ant_data) >= 3:  # Solo formiche con almeno 3 viaggi
                ant_data['velocity_ma_ant'] = ant_data['velocity'].rolling(window=min(window//5, len(ant_data)), center=True, min_periods=1).mean()
                ant_data['efficiency_ma_ant'] = ant_data['efficiency'].rolling(window=min(window//5, len(ant_data)), center=True, min_periods=1).mean()
                ant_ma_data.append(ant_data)
        
        if ant_ma_data:
            ant_combined = pd.concat(ant_ma_data)
            # Calcola trend medio: media delle medie mobili per-formica
            time_bins = np.arange(0, 101, 2)  # Bins ogni 2%
            trend_mean_vel = []
            trend_mean_eff = []
            
            for i in range(len(time_bins)-1):
                bin_start = time_bins[i]
                bin_end = time_bins[i+1]
                bin_data = ant_combined[
                    (ant_combined['time_percent'] >= bin_start) & 
                    (ant_combined['time_percent'] < bin_end)
                ]
                if len(bin_data) > 0:
                    trend_mean_vel.append(bin_data['velocity_ma_ant'].mean())
                    trend_mean_eff.append(bin_data['efficiency_ma_ant'].mean())
                else:
                    trend_mean_vel.append(np.nan)
                    trend_mean_eff.append(np.nan)
            
            # Interpola valori mancanti
            trend_mean_vel = pd.Series(trend_mean_vel).interpolate().values
            trend_mean_eff = pd.Series(trend_mean_eff).interpolate().values
            
            self.trend_time_bins = time_bins[:-1]
            self.trend_mean_velocity = trend_mean_vel
            self.trend_mean_efficiency = trend_mean_eff
        else:
            self.trend_time_bins = np.array([])
            self.trend_mean_velocity = np.array([])
            self.trend_mean_efficiency = np.array([])
        
        self.performance_metrics = df
    
    def create_line_plot(self):
        """
        Crea il line plot con punti individuali e medie mobili
        """
        print("Creazione line plot...")
        
        df = self.performance_metrics
        
        fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(14, 10))
        fig.suptitle('Declino Temporale delle Performance delle Formiche', fontsize=16, fontweight='bold')
        
        # Grafico 1: Velocità
        # Punti individuali
        ax1.scatter(df['time_percent'], df['velocity'], alpha=0.3, s=20, color='lightblue', label='Viaggi individuali')
        # Trend globale
        ax1.plot(df['time_percent'], df['velocity_ma'], color='blue', linewidth=3, label='Trend globale (media mobile)')
        # Trend medio per-formica
        if len(self.trend_mean_velocity) > 0:
            ax1.plot(self.trend_time_bins, self.trend_mean_velocity, color='red', linewidth=2, linestyle='--', label='Trend medio per-formica')
        
        # Linea divisoria 50%
        ax1.axvline(x=50, color='gray', linestyle=':', alpha=0.7, label='Divisione prima/seconda metà')
        
        ax1.set_xlabel('Avanzamento Simulazione (%)')
        ax1.set_ylabel('Velocità (pixel/sec)')
        ax1.set_title('Velocità nel Tempo')
        ax1.grid(True, alpha=0.3)
        ax1.legend()
        
        # Annotazione declino
        ax1.annotate(f'Declino: {self.velocity_decline:.1f}%', 
                    xy=(50, ax1.get_ylim()[1] * 0.9), fontsize=12, 
                    bbox=dict(boxstyle="round,pad=0.3", facecolor="yellow", alpha=0.7))
        
        # Grafico 2: Efficienza
        # Punti individuali
        ax2.scatter(df['time_percent'], df['efficiency'], alpha=0.3, s=20, color='lightgreen', label='Viaggi individuali')
        # Trend globale
        ax2.plot(df['time_percent'], df['efficiency_ma'], color='darkgreen', linewidth=3, label='Trend globale (media mobile)')
        # Trend medio per-formica
        if len(self.trend_mean_efficiency) > 0:
            ax2.plot(self.trend_time_bins, self.trend_mean_efficiency, color='red', linewidth=2, linestyle='--', label='Trend medio per-formica')
        
        # Linea divisoria 50%
        ax2.axvline(x=50, color='gray', linestyle=':', alpha=0.7, label='Divisione prima/seconda metà')
        
        ax2.set_xlabel('Avanzamento Simulazione (%)')
        ax2.set_ylabel('Efficienza (pixel/sec^1.5)')
        ax2.set_title('Efficienza nel Tempo')
        ax2.grid(True, alpha=0.3)
        ax2.legend()
        
        # Annotazione declino
        ax2.annotate(f'Declino: {self.efficiency_decline:.1f}%', 
                    xy=(50, ax2.get_ylim()[1] * 0.9), fontsize=12, 
                    bbox=dict(boxstyle="round,pad=0.3", facecolor="yellow", alpha=0.7))
        
        plt.tight_layout()
        
        # Salva grafico
        output_path = self.csv_path.parent / f"{self.csv_path.stem}_line_plot.png"
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"Line plot salvato: {output_path}")
        
        plt.show()
    
    def create_bar_chart(self):
        """
        Crea il bar chart before/after
        """
        print("Creazione bar chart...")
        
        df = self.performance_metrics
        
        # Dividi in prima e seconda metà
        first_half = df[df['time_percent'] <= 50]
        second_half = df[df['time_percent'] > 50]
        
        if len(first_half) == 0 or len(second_half) == 0:
            print("Errore: Non ci sono abbastanza dati per creare il bar chart")
            return
        
        # Calcola medie
        metrics = {
            'Prima Metà': {
                'Velocità': first_half['velocity'].mean(),
                'Efficienza': first_half['efficiency'].mean()
            },
            'Seconda Metà': {
                'Velocità': second_half['velocity'].mean(),
                'Efficienza': second_half['efficiency'].mean()
            }
        }
        
        # Crea grafico
        fig, ax = plt.subplots(1, 1, figsize=(12, 8))
        
        x = np.arange(2)  # 2 gruppi (velocità, efficienza)
        width = 0.35
        
        # Dati per le barre
        first_half_values = [metrics['Prima Metà']['Velocità'], metrics['Prima Metà']['Efficienza']]
        second_half_values = [metrics['Seconda Metà']['Velocità'], metrics['Seconda Metà']['Efficienza']]
        
        # Crea barre
        bars1 = ax.bar(x - width/2, first_half_values, width, label='Prima Metà (0-50%)', color='skyblue', alpha=0.8)
        bars2 = ax.bar(x + width/2, second_half_values, width, label='Seconda Metà (50-100%)', color='lightcoral', alpha=0.8)
        
        # Aggiungi valori sopra le barre
        for i, (bar1, bar2) in enumerate(zip(bars1, bars2)):
            height1 = bar1.get_height()
            height2 = bar2.get_height()
            
            ax.text(bar1.get_x() + bar1.get_width()/2., height1 + height1*0.01,
                   f'{height1:.1f}', ha='center', va='bottom', fontweight='bold')
            
            ax.text(bar2.get_x() + bar2.get_width()/2., height2 + height2*0.01,
                   f'{height2:.1f}', ha='center', va='bottom', fontweight='bold')
            
            # Calcola e mostra percentuale di declino
            decline = ((height2 - height1) / height1) * 100
            metric_name = ['Velocità', 'Efficienza'][i]
            
            # Freccia verso il basso per mostrare declino
            if decline < 0:
                ax.annotate('', xy=(i, height1), xytext=(i, height2),
                           arrowprops=dict(arrowstyle='<->', color='red', lw=2))
        
        # Formattazione
        ax.set_xlabel('Metriche di Performance')
        ax.set_ylabel('Valore')
        ax.set_title('Confronto Performance: Prima vs Seconda Metà della Simulazione')
        ax.set_xticks(x)
        ax.set_xticklabels(['Velocità\n(pixel/sec)', 'Efficienza\n(pixel/sec^1.5)'])
        ax.legend()
        ax.grid(True, alpha=0.3, axis='y')
        
        # Aggiungi testo esplicativo
        #textstr = f'Declino Velocità: {self.velocity_decline:.1f}%\nDeclino Efficienza: {self.efficiency_decline:.1f}%'
        #props = dict(boxstyle='round', facecolor='wheat', alpha=0.8)
        #ax.text(0.02, 0.98, textstr, transform=ax.transAxes, fontsize=10,
        #       verticalalignment='top', bbox=props)
        
        plt.tight_layout()
        
        # Salva grafico
        output_path = self.csv_path.parent / f"{self.csv_path.stem}_bar_chart.png"
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"Bar chart salvato: {output_path}")
        
        plt.show()
    
    def print_summary(self):
        """
        Stampa riassunto dei risultati
        """
        print("\n" + "="*60)
        print(" RIASSUNTO ANALISI PERFORMANCE TEMPORALE")
        print("="*60)
        
        df = self.performance_metrics
        
        print(f"Viaggi analizzati: {len(df)}")
        print(f"Formiche uniche: {df['ant_id'].nunique()}")
        print(f"Durata simulazione: {(df['pickup_time'].max() - df['pickup_time'].min()) / 1e9:.1f} secondi")
        
        print(f"\nStatistiche globali:")
        print(f"Distanza media: {df['distance'].mean():.1f} pixel")
        print(f"Velocità media: {df['velocity'].mean():.1f} pixel/sec")
        print(f"Efficienza media: {df['efficiency'].mean():.1f} pixel/sec^1.5")
        
        print(f"\nDeclino temporale:")
        print(f"Velocità: {self.velocity_decline:.2f}%")
        print(f"Efficienza: {self.efficiency_decline:.2f}%")
        
        # Validazione precisione calcoli
        distances = df['distance'].values
        valid_distances = distances[distances > 0]
        print(f"\nValidazione calcoli:")
        print(f"Viaggi con distanza > 0: {len(valid_distances)}/{len(distances)}")
        print(f"Distanza min/max: {valid_distances.min():.1f} / {valid_distances.max():.1f} pixel")
        
        print("="*60)
    
    def run_full_analysis(self):
        """
        Esegue l'analisi completa
        """
        start_time = time.time()
        print("Avvio analisi completa performance formiche...")
        
        try:
            self.load_and_filter_csv()
            self.extract_trips()
            self.calculate_precise_distances()
            self.prepare_temporal_analysis()
            self.calculate_moving_averages(window=50)
            
            self.create_line_plot()
            self.create_bar_chart()
            
            self.print_summary()
            
            total_time = time.time() - start_time
            print(f"\nAnalisi completata in {total_time:.1f} secondi")
            
        except Exception as e:
            print(f"Errore durante l'analisi: {e}")
            raise


def main():
    parser = argparse.ArgumentParser(description='Analisi performance temporale formiche con calcolo distanze precise')
    parser.add_argument('csv_file', help='Percorso del file CSV con i dati')
    
    args = parser.parse_args()
    
    if not Path(args.csv_file).exists():
        print(f"Errore: File '{args.csv_file}' non trovato")
        return
    
    analyzer = AntPerformanceAnalyzer(args.csv_file)
    analyzer.run_full_analysis()


if __name__ == "__main__":
    import sys
    if len(sys.argv) == 1:
        print("Utilizzo: python script.py <file.csv>")
        sys.exit(1)
    
    main()