#!/usr/bin/env python3
"""
Script per l'analisi del foraging delle formiche
Genera grafici interattivi per durata cicli e efficienza normalizzata
"""

import re
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import plotly.graph_objects as go
import plotly.subplots as sp
from plotly.offline import plot
import argparse
from pathlib import Path
from typing import Dict, List, Tuple
import warnings

warnings.filterwarnings('ignore')

class AntForagingAnalyzer:
    def __init__(self, txt_file_path: str):
        """
        Inizializza l'analizzatore con il percorso del file di testo
        """
        self.file_path = Path(txt_file_path)
        self.raw_data = {}
        self.efficiency_data = {}
        self.processed_data = {}
        
    def parse_txt_file(self):
        """
        Parsing del file TXT nel formato specificato con gestione encoding robusto
        """
        print("Parsing del file di testo...")
        
        # Prova diversi encoding per massima compatibilità
        encodings_to_try = ['utf-8', 'latin-1', 'cp1252', 'iso-8859-1', 'utf-16']
        content = None
        
        for encoding in encodings_to_try:
            try:
                with open(self.file_path, 'r', encoding=encoding, errors='ignore') as f:
                    content = f.read()
                print(f"File letto con encoding: {encoding}")
                break
            except UnicodeDecodeError:
                continue
        
        if content is None:
            raise ValueError("Impossibile leggere il file con nessun encoding supportato")
        
        # Estrai gli eventi di foraging (pickup/drop)
        foraging_section = re.search(
            r'ANALISI EVENTI FORAGING:(.*?)(?=ANALISI EFFICIENZA DETTAGLIATA:|$)', 
            content, re.DOTALL
        )
        
        if foraging_section:
            self._parse_foraging_events(foraging_section.group(1))
        
        # Estrai i dati di efficienza
        efficiency_section = re.search(
            r'ANALISI EFFICIENZA DETTAGLIATA:(.*?)(?=STATISTICHE GLOBALI:|$)', 
            content, re.DOTALL
        )
        
        if efficiency_section:
            self._parse_efficiency_data(efficiency_section.group(1))
        
        print(f"Trovate {len(self.raw_data)} formiche con dati di foraging")
        
    def _parse_foraging_events(self, foraging_text: str):
        """
        Estrae gli eventi pickup/drop per ogni formica
        """
        # Pattern per identificare le formiche
        ant_sections = re.findall(r'formica (\d+):(.*?)(?=formica \d+:|$)', foraging_text, re.DOTALL)
        
        for ant_id, ant_data in ant_sections:
            ant_id = int(ant_id)
            self.raw_data[ant_id] = {'pickups': [], 'drops': []}
            
            # Estrai timestamps pickup
            pickups = re.findall(r'pickup: (\d+)', ant_data)
            self.raw_data[ant_id]['pickups'] = [int(p) for p in pickups]
            
            # Estrai timestamps drop
            drops = re.findall(r'drop: (\d+)', ant_data)
            self.raw_data[ant_id]['drops'] = [int(d) for d in drops]
            
    def _parse_efficiency_data(self, efficiency_text: str):
        """
        Estrae i dati di efficienza per ogni formica
        """
        ant_sections = re.findall(r'formica (\d+):(.*?)(?=formica \d+:|$)', efficiency_text, re.DOTALL)
        
        for ant_id, ant_data in ant_sections:
            ant_id = int(ant_id)
            
            # Estrai efficienza normalizzata temporalmente se presente
            norm_eff_match = re.search(r'Efficienza normalizzata temporalmente.*?Media: ([\d.]+)', ant_data, re.DOTALL)
            if norm_eff_match:
                self.efficiency_data[ant_id] = {'normalized_efficiency': float(norm_eff_match.group(1))}
            else:
                self.efficiency_data[ant_id] = {'normalized_efficiency': None}
    
    def process_data(self):
        """
        Processa i dati grezzi calcolando metriche e timeline
        """
        print("Processamento dei dati...")
        
        all_timestamps = []
        
        for ant_id in self.raw_data.keys():
            pickups = self.raw_data[ant_id]['pickups']
            drops = self.raw_data[ant_id]['drops']
            
            # Assicurati che pickup e drop abbiano lo stesso numero
            min_length = min(len(pickups), len(drops))
            if min_length == 0:
                continue
                
            pickups = pickups[:min_length]
            drops = drops[:min_length]
            
            # Converti in secondi (da nanosecondi)
            pickups_sec = [p / 1e9 for p in pickups]
            drops_sec = [d / 1e9 for d in drops]
            
            # Calcola durate cicli
            cycle_durations = [drop - pickup for pickup, drop in zip(pickups_sec, drops_sec)]
            
            # Calcola timestamp relativi (tempo dall'inizio simulazione)
            all_timestamps.extend(pickups_sec + drops_sec)
            
            self.processed_data[ant_id] = {
                'pickups_sec': pickups_sec,
                'drops_sec': drops_sec,
                'cycle_durations': cycle_durations,
                'normalized_efficiency': self.efficiency_data.get(ant_id, {}).get('normalized_efficiency', None)
            }
        
        # Calcola tempo di inizio simulazione (primo pickup)
        if all_timestamps:
            self.simulation_start = min(all_timestamps)
            self.simulation_end = max(all_timestamps)
            self.total_duration = self.simulation_end - self.simulation_start
            
            # Calcola tempi relativi per ogni formica
            for ant_id in self.processed_data.keys():
                pickups_rel = [(p - self.simulation_start) for p in self.processed_data[ant_id]['pickups_sec']]
                drops_rel = [(d - self.simulation_start) for d in self.processed_data[ant_id]['drops_sec']]
                
                self.processed_data[ant_id]['pickups_rel'] = pickups_rel
                self.processed_data[ant_id]['drops_rel'] = drops_rel
                self.processed_data[ant_id]['trip_centers'] = [(p + d) / 2 for p, d in zip(pickups_rel, drops_rel)]
        
        print(f"Dati processati. Durata simulazione: {self.total_duration:.1f} secondi")
    
    def calculate_temporal_normalized_efficiency(self):
        """
        Calcola l'efficienza normalizzata temporalmente come nello script originale
        """
        print("Calcolo efficienza normalizzata temporalmente...")
        
        # Raccogli tutti i dati con timestamp per finestra mobile
        all_data = []
        for ant_id, data in self.processed_data.items():
            for i, (pickup_rel, duration) in enumerate(zip(data['pickups_rel'], data['cycle_durations'])):
                if duration > 0:
                    # Assumiamo distanza fissa per semplicità (o potresti estrarla dai dati)
                    # In assenza di dati di distanza, usiamo l'inverso della durata come proxy di velocità
                    speed = 1.0 / duration  # Proxy: formiche più veloci hanno durate minori
                    all_data.append({
                        'ant_id': ant_id,
                        'timestamp': pickup_rel,
                        'speed': speed,
                        'trip_index': i
                    })
        
        # Ordina per timestamp
        all_data.sort(key=lambda x: x['timestamp'])
        
        # Calcola efficienza normalizzata per ogni formica
        for ant_id in self.processed_data.keys():
            ant_data = [d for d in all_data if d['ant_id'] == ant_id]
            if not ant_data:
                continue
                
            normalized_values = []
            for data_point in ant_data:
                # Finestra temporale (±10% del tempo totale)
                window_size = self.total_duration * 0.1
                window_start = max(0, data_point['timestamp'] - window_size)
                window_end = min(self.total_duration, data_point['timestamp'] + window_size)
                
                # Trova velocità media nella finestra
                window_speeds = [d['speed'] for d in all_data 
                               if window_start <= d['timestamp'] <= window_end]
                
                if window_speeds:
                    reference_speed = np.mean(window_speeds)
                    if reference_speed > 0:
                        normalized = data_point['speed'] / reference_speed
                        normalized_values.append(normalized)
                    else:
                        normalized_values.append(1.0)
                else:
                    normalized_values.append(1.0)
            
            self.processed_data[ant_id]['temporal_normalized_efficiency'] = normalized_values


    def calculate_global_efficiency_stats(self):
        """
        Calcola statistiche globali dell'efficienza normalizzata (media e mediana)
        """
        print("Calcolo statistiche globali efficienza normalizzata...")
        
        # Raccogli tutti i valori di efficienza normalizzata
        all_normalized_efficiency = []
        for ant_id, data in self.processed_data.items():
            if 'temporal_normalized_efficiency' in data and data['temporal_normalized_efficiency']:
                all_normalized_efficiency.extend(data['temporal_normalized_efficiency'])
        
        if all_normalized_efficiency:
            self.global_efficiency_mean = np.mean(all_normalized_efficiency)
            self.global_efficiency_median = np.median(all_normalized_efficiency)
            self.global_efficiency_values = all_normalized_efficiency
            
            print(f"Efficienza normalizzata globale:")
            print(f"  Media: {self.global_efficiency_mean:.3f}")
            print(f"  Mediana: {self.global_efficiency_median:.3f}")
            print(f"  Valori totali: {len(all_normalized_efficiency)}")
        else:
            self.global_efficiency_mean = None
            self.global_efficiency_median = None
            self.global_efficiency_values = []
    
    def select_representative_ants(self, max_ants=5):
        """
        Seleziona formiche rappresentative di diversi percentili di performance
        """
        print(f"Selezione di {max_ants} formiche rappresentative da {len(self.processed_data)}...")
        
        if len(self.processed_data) <= max_ants:
            return list(self.processed_data.keys())
        
        # Calcola durata media per ogni formica
        ant_avg_durations = {}
        for ant_id, data in self.processed_data.items():
            if data.get('cycle_durations'):
                ant_avg_durations[ant_id] = np.mean(data['cycle_durations'])
        
        if not ant_avg_durations:
            return list(self.processed_data.keys())[:max_ants]
        
        # Ordina le formiche per performance (durata media crescente)
        sorted_ants = sorted(ant_avg_durations.items(), key=lambda x: x[1])
        
        # Seleziona formiche dai percentili distribuiti uniformemente
        if max_ants == 5:
            # Per 5 formiche: percentili 10, 30, 50, 70, 90
            percentiles = [10, 30, 50, 70, 90]
        else:
            # Per altri numeri, distribuzione uniforme
            percentiles = np.linspace(5, 95, max_ants)
        
        selected_ants = []
        total_ants = len(sorted_ants)
        
        for p in percentiles:
            index = int((p / 100.0) * (total_ants - 1))
            index = max(0, min(index, total_ants - 1))
            ant_id, avg_duration = sorted_ants[index]
            selected_ants.append(ant_id)
        
        print(f"Selezionate {len(selected_ants)} formiche rappresentative per percentili:")
        
        # Stampa informazioni sulle formiche selezionate
        for i, ant_id in enumerate(selected_ants):
            avg_duration = ant_avg_durations[ant_id]
            percentile = percentiles[i] if i < len(percentiles) else 50
            data = self.processed_data[ant_id]
            num_trips = len(data.get('cycle_durations', []))
            
            print(f"  {percentile:2.0f}° percentile - Formica {ant_id}: {avg_duration:.2f}s media, {num_trips} viaggi")
        
        return selected_ants
    
    def create_matplotlib_plots(self):
        """
        Crea grafici statici con matplotlib usando solo formiche rappresentative
        """
        print("Creazione grafici matplotlib...")
        
        # Seleziona formiche rappresentative
        representative_ants = self.select_representative_ants(max_ants=5)
        
        fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(16, 12))
        fig.suptitle(f'Analisi Foraging delle Formiche (Mostrate {len(representative_ants)}/{len(self.processed_data)})', 
                    fontsize=16, fontweight='bold')
        
        # Palette colori per le formiche selezionate
        colors = plt.cm.tab20(np.linspace(0, 1, len(representative_ants)))
        
        # Grafico 1: Durata media cicli nel tempo
        for i, ant_id in enumerate(representative_ants):
            data = self.processed_data.get(ant_id, {})
            if data and data.get('cycle_durations'):
                ax1.plot(data['trip_centers'], data['cycle_durations'], 
                        'o-', alpha=0.8, label=f'Formica {ant_id}', 
                        linewidth=2, markersize=4, color=colors[i])
        
        ax1.set_xlabel('Tempo dalla simulazione (sec)')
        ax1.set_ylabel('Durata Ciclo Pickup-Drop (sec)')
        ax1.set_title('Durata Cicli nel Tempo (Formiche Rappresentative)')
        ax1.grid(True, alpha=0.3)
        ax1.legend(bbox_to_anchor=(1.05, 1), loc='upper left', fontsize=8)
        
        # Grafico 2: Media mobile delle durate
        window = 3
        for i, ant_id in enumerate(representative_ants):
            data = self.processed_data.get(ant_id, {})
            if data and len(data.get('cycle_durations', [])) >= window:
                smoothed = pd.Series(data['cycle_durations']).rolling(window, center=True).mean()
                ax2.plot(data['trip_centers'], smoothed, 
                        '-', alpha=0.8, label=f'Formica {ant_id}', 
                        linewidth=2.5, color=colors[i])
        
        ax2.set_xlabel('Tempo dalla simulazione (sec)')
        ax2.set_ylabel('Durata Media Mobile (sec)')
        ax2.set_title(f'Trend Durate (Media Mobile {window} punti)')
        ax2.grid(True, alpha=0.3)
        
        # Grafico 3: Efficienza normalizzata nel tempo
        for i, ant_id in enumerate(representative_ants):
            data = self.processed_data.get(ant_id, {})
            if (data and 'temporal_normalized_efficiency' in data 
                and data['temporal_normalized_efficiency']):
                trip_centers = data['trip_centers'][:len(data['temporal_normalized_efficiency'])]
                ax3.plot(trip_centers, data['temporal_normalized_efficiency'], 
                        'o-', alpha=0.8, label=f'Formica {ant_id}', 
                        linewidth=2, markersize=4, color=colors[i])
        
        # Calcola statistiche reali efficienza normalizzata da TUTTE le formiche
        all_normalized_efficiency = []
        for ant_data in self.processed_data.values():
            if 'temporal_normalized_efficiency' in ant_data and ant_data['temporal_normalized_efficiency']:
                all_normalized_efficiency.extend(ant_data['temporal_normalized_efficiency'])
        
        # Linee di riferimento con valori reali calcolati
        if all_normalized_efficiency:
            mean_efficiency = np.mean(all_normalized_efficiency)
            median_efficiency = np.median(all_normalized_efficiency)

            print(f"Valori globali efficienza normalizzata calcolati: Media={mean_efficiency:.3f}, Mediana={median_efficiency:.3f}")
            
            ax3.axhline(y=mean_efficiency, color='red', linestyle='--', alpha=0.7, linewidth=2, 
                    label=f'Media ({mean_efficiency:.3f})')
            ax3.axhline(y=median_efficiency, color='green', linestyle=':', alpha=0.7, linewidth=2, 
                    label=f'Mediana ({median_efficiency:.3f})')
            
            print(f"Efficienza normalizzata globale - Media: {mean_efficiency:.3f}, Mediana: {median_efficiency:.3f}")
        
        ax3.set_xlabel('Tempo dalla simulazione (sec)')
        ax3.set_ylabel('Efficienza Normalizzata')
        ax3.set_title('Efficienza Normalizzata nel Tempo')
        ax3.grid(True, alpha=0.3)
        ax3.legend(bbox_to_anchor=(1.05, 1), loc='upper left', fontsize=8)
        
        # Grafico 4: Distribuzione durate per formica rappresentativa
        durations_by_ant = {}
        for ant_id in representative_ants:
            data = self.processed_data.get(ant_id, {})
            if data and data.get('cycle_durations'):
                durations_by_ant[ant_id] = data['cycle_durations']
        
        if durations_by_ant:
            bp = ax4.boxplot([durations for durations in durations_by_ant.values()], 
                        labels=[f'F{ant_id}' for ant_id in durations_by_ant.keys()],
                        patch_artist=True)
            
            # Colora i box plot con gli stessi colori delle linee
            ant_to_color = {ant_id: colors[i] for i, ant_id in enumerate(representative_ants)}
            for patch, ant_id in zip(bp['boxes'], durations_by_ant.keys()):
                if ant_id in ant_to_color:
                    patch.set_facecolor(ant_to_color[ant_id])
                    patch.set_alpha(0.7)
        
        ax4.set_xlabel('Formiche Rappresentative')
        ax4.set_ylabel('Durata Ciclo (sec)')
        ax4.set_title('Distribuzione Durate (Formiche Selezionate)')
        ax4.grid(True, alpha=0.3)
        ax4.tick_params(axis='x', rotation=45)
        
        plt.tight_layout()
        
        # Salva il grafico
        output_path = self.file_path.parent / f"{self.file_path.stem}_analysis.png"
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"Grafico salvato: {output_path}")
        
        plt.show()

    def create_interactive_plots(self):
        """
        Crea grafici interattivi con Plotly usando solo formiche rappresentative
        """
        print("Creazione grafici interattivi...")
        
        # Seleziona formiche rappresentative
        representative_ants = self.select_representative_ants(max_ants=5)
        
        # Crea subplot con 2x2 layout
        fig = sp.make_subplots(
            rows=2, cols=2,
            subplot_titles=(
                f'Durata Cicli nel Tempo ({len(representative_ants)} formiche rappresentative)',
                'Efficienza Normalizzata nel Tempo',
                'Confronto Prestazioni per Formica',
                'Timeline Completa Attivita'
            ),
            specs=[[{"secondary_y": False}, {"secondary_y": False}],
                [{"secondary_y": False}, {"secondary_y": False}]]
        )
        
        # Palette colori distintivi per le formiche selezionate
        colors = plt.cm.tab20(np.linspace(0, 1, len(representative_ants)))
        color_map = {ant_id: f'rgb({int(c[0]*255)},{int(c[1]*255)},{int(c[2]*255)})' 
                    for ant_id, c in zip(representative_ants, colors)}
        
        # Grafico 1: Durata cicli (con possibilità di on/off per formica)
        for ant_id in representative_ants:
            data = self.processed_data.get(ant_id, {})
            if data and data.get('cycle_durations'):
                fig.add_trace(
                    go.Scatter(
                        x=data['trip_centers'],
                        y=data['cycle_durations'],
                        mode='lines+markers',
                        name=f'Formica {ant_id}',
                        line=dict(color=color_map[ant_id], width=2),
                        marker=dict(size=6),
                        hovertemplate=f'<b>Formica {ant_id}</b><br>' +
                                    'Tempo: %{x:.1f}s<br>' +
                                    'Durata: %{y:.2f}s<br>' +
                                    f'Viaggi: {len(data["cycle_durations"])}<extra></extra>',
                        legendgroup=f'ant_{ant_id}'
                    ),
                    row=1, col=1
                )
        
        # Grafico 2: Efficienza normalizzata
        for ant_id in representative_ants:
            data = self.processed_data.get(ant_id, {})
            if (data and 'temporal_normalized_efficiency' in data 
                and data['temporal_normalized_efficiency']):
                trip_centers = data['trip_centers'][:len(data['temporal_normalized_efficiency'])]
                fig.add_trace(
                    go.Scatter(
                        x=trip_centers,
                        y=data['temporal_normalized_efficiency'],
                        mode='lines+markers',
                        name=f'Formica {ant_id}',
                        line=dict(color=color_map[ant_id], width=2),
                        marker=dict(size=6),
                        showlegend=False,
                        hovertemplate=f'<b>Formica {ant_id}</b><br>' +
                                    'Tempo: %{x:.1f}s<br>' +
                                    'Efficienza Norm.: %{y:.3f}<extra></extra>',
                        legendgroup=f'ant_{ant_id}'
                    ),
                    row=1, col=2
                )
        
        # Calcola statistiche reali efficienza normalizzata da TUTTE le formiche  
        all_normalized_efficiency = []
        for ant_data in self.processed_data.values():
            if 'temporal_normalized_efficiency' in ant_data and ant_data['temporal_normalized_efficiency']:
                all_normalized_efficiency.extend(ant_data['temporal_normalized_efficiency'])
        
        # Linee di riferimento con valori reali calcolati
        if all_normalized_efficiency:
            mean_efficiency = np.mean(all_normalized_efficiency)
            median_efficiency = np.median(all_normalized_efficiency)
            
            fig.add_hline(y=mean_efficiency, line_dash="dash", line_color="red", 
                        annotation_text=f"Media ({mean_efficiency:.3f})", annotation_position="top right", row=1, col=2)
            fig.add_hline(y=median_efficiency, line_dash="dot", line_color="green", 
                        annotation_text=f"Mediana ({median_efficiency:.3f})", annotation_position="bottom right", row=1, col=2)
        
        # Grafico 3: Box plot prestazioni
        for ant_id in representative_ants:
            data = self.processed_data.get(ant_id, {})
            if data and data.get('cycle_durations'):
                fig.add_trace(
                    go.Box(
                        y=data['cycle_durations'],
                        name=f'F{ant_id}',
                        boxpoints='outliers',
                        marker_color=color_map[ant_id],
                        showlegend=False,
                        hovertemplate=f'<b>Formica {ant_id}</b><br>' +
                                    f'Viaggi: {len(data["cycle_durations"])}<br>' +
                                    f'Media: {np.mean(data["cycle_durations"]):.2f}s<br>' +
                                    f'Mediana: {np.median(data["cycle_durations"]):.2f}s<extra></extra>'
                    ),
                    row=2, col=1
                )
        
        # Grafico 4: Timeline attività (Gantt-like) - tutte le 5 formiche
        timeline_ants = representative_ants
        
        for i, ant_id in enumerate(timeline_ants):
            data = self.processed_data.get(ant_id, {})
            if data and data.get('pickups_rel') and data.get('drops_rel'):
                y_position = i + 1  # Posizione verticale per questa formica
                
                # Crea barre per ogni ciclo
                for j, (start, end) in enumerate(zip(data['pickups_rel'], data['drops_rel'])):
                    fig.add_trace(
                        go.Scatter(
                            x=[start, end, end, start, start],
                            y=[y_position-0.4, y_position-0.4, y_position+0.4, y_position+0.4, y_position-0.4],
                            fill="toself",
                            fillcolor=color_map[ant_id],
                            opacity=0.6,
                            line=dict(color=color_map[ant_id], width=1),
                            name=f'Formica {ant_id}',
                            showlegend=False,
                            hovertemplate=f'<b>Formica {ant_id} - Viaggio {j+1}</b><br>' +
                                        'Inizio: %{x[0]:.1f}s<br>' +
                                        'Fine: %{x[1]:.1f}s<br>' +
                                        f'Durata: {end-start:.2f}s<extra></extra>',
                            legendgroup=f'ant_{ant_id}'
                        ),
                        row=2, col=2
                    )
        
        # Aggiorna layout
        fig.update_layout(
            title=dict(
                text=f"<b>Analisi Interattiva Foraging delle Formiche</b><br>" +
                    f"<sub>Durata simulazione: {self.total_duration:.1f}s | " +
                    f"Mostrate {len(representative_ants)} formiche rappresentative su {len(self.processed_data)} totali</sub>",
                x=0.5,
                font=dict(size=16)
            ),
            height=800,
            showlegend=True,
            legend=dict(
                orientation="v",
                yanchor="top",
                y=1,
                xanchor="left",
                x=1.05
            ),
            hovermode='closest'
        )
        
        # Aggiorna assi
        fig.update_xaxes(title_text="Tempo dalla simulazione (s)", row=1, col=1)
        fig.update_yaxes(title_text="Durata Ciclo (s)", row=1, col=1)
        
        fig.update_xaxes(title_text="Tempo dalla simulazione (s)", row=1, col=2)
        fig.update_yaxes(title_text="Efficienza Normalizzata", row=1, col=2)
        
        fig.update_xaxes(title_text="Formiche Rappresentative", row=2, col=1)
        fig.update_yaxes(title_text="Durata Ciclo (s)", row=2, col=1)
        
        fig.update_xaxes(title_text="Tempo dalla simulazione (s)", row=2, col=2)
        fig.update_yaxes(title_text="Formiche", tickvals=list(range(1, len(timeline_ants)+1)), 
                        ticktext=[f'F{ant_id}' for ant_id in timeline_ants], row=2, col=2)
        
        # Salva file HTML interattivo
        output_path = self.file_path.parent / f"{self.file_path.stem}_interactive.html"
        plot(fig, filename=str(output_path), auto_open=True)
        print(f"Grafico interattivo salvato: {output_path}")
    
    def print_summary(self):
        """
        Stampa un riassunto dei risultati
        """
        print("\n" + "="*60)
        print(" RIASSUNTO ANALISI FORAGING")
        print("="*60)
        
        total_cycles = sum(len(data['cycle_durations']) for data in self.processed_data.values())
        if total_cycles > 0:
            avg_duration = np.mean([d for data in self.processed_data.values() for d in data['cycle_durations']])
        else:
            avg_duration = 0
        
        print(f"Formiche totali nel file: {len(self.processed_data)}")
        print(f"Cicli totali analizzati: {total_cycles}")
        print(f"Durata media ciclo: {avg_duration:.2f} secondi")
        print(f"Durata simulazione: {self.total_duration:.1f} secondi")
        
        # Statistiche sui gruppi comportamentali
        representative_ants = self.select_representative_ants(max_ants=5)
        print(f"\nFormiche mostrate nei grafici: {len(representative_ants)} (percentili rappresentativi)")
        
        # Formica più e meno efficiente
        ant_avg_durations = {}
        for ant_id, data in self.processed_data.items():
            if data['cycle_durations']:
                ant_avg_durations[ant_id] = np.mean(data['cycle_durations'])
        
        if ant_avg_durations:
            best_ant = min(ant_avg_durations, key=ant_avg_durations.get)
            worst_ant = max(ant_avg_durations, key=ant_avg_durations.get)
            
            print(f"Formica piu veloce (globale): {best_ant} (avg: {ant_avg_durations[best_ant]:.2f}s)")
            print(f"Formica piu lenta (globale): {worst_ant} (avg: {ant_avg_durations[worst_ant]:.2f}s)")
            print(f"Rapporto velocita: {ant_avg_durations[worst_ant]/ant_avg_durations[best_ant]:.1f}x")
        
        # Statistiche di dispersione
        all_durations = [d for data in self.processed_data.values() for d in data['cycle_durations']]
        if all_durations:
            print(f"\nDistribuzione durate globale:")
            print(f"  Min: {np.min(all_durations):.2f}s")
            print(f"  25%: {np.percentile(all_durations, 25):.2f}s")
            print(f"  50% (mediana): {np.percentile(all_durations, 50):.2f}s")
            print(f"  75%: {np.percentile(all_durations, 75):.2f}s")
            print(f"  Max: {np.max(all_durations):.2f}s")
            print(f"  Deviazione std: {np.std(all_durations):.2f}s")

        # Statistiche efficienza normalizzata globale
        if hasattr(self, 'global_efficiency_mean') and self.global_efficiency_mean is not None:
            print(f"\nEfficienza Normalizzata Globale:")
            print(f"  Media: {self.global_efficiency_mean:.3f}")
            print(f"  Mediana: {self.global_efficiency_median:.3f}")
            
            if self.global_efficiency_median > self.global_efficiency_mean:
                print(f"  La mediana ({self.global_efficiency_median:.3f}) è superiore alla media ({self.global_efficiency_mean:.3f})")
                print(f"  Questo indica che la maggioranza performa sopra la media")
            
            # Calcola percentuale valori sopra/sotto media
            values = np.array(self.global_efficiency_values)
            above_mean = np.sum(values > self.global_efficiency_mean)
            below_mean = np.sum(values < self.global_efficiency_mean)
            total = len(values)
            
            print(f"  Viaggi sopra la media: {above_mean}/{total} ({above_mean/total*100:.1f}%)")
            print(f"  Viaggi sotto la media: {below_mean}/{total} ({below_mean/total*100:.1f}%)")
        
        print("="*60)
        print("NOTA: I grafici mostrano solo formiche rappresentative")
        print("      selezionate per diversi pattern comportamentali")
        print("="*60)
    
    def run_analysis(self):
        """
        Esegue l'analisi completa
        """
        print("Avvio analisi completa del foraging...")
        print(f"File: {self.file_path}")
        
        self.parse_txt_file()
        self.process_data()
        self.calculate_temporal_normalized_efficiency()
        self.calculate_global_efficiency_stats()
        
        self.create_matplotlib_plots()
        self.create_interactive_plots()
        
        self.print_summary()
        
        print("Analisi completata con successo!")


def main():
    parser = argparse.ArgumentParser(description='Analisi del foraging delle formiche')
    parser.add_argument('file', help='Percorso del file TXT con i dati')
    parser.add_argument('--only-static', action='store_true', 
                       help='Genera solo grafici statici (matplotlib)')
    parser.add_argument('--only-interactive', action='store_true', 
                       help='Genera solo grafici interattivi (plotly)')
    
    args = parser.parse_args()
    
    if not Path(args.file).exists():
        print(f"Errore: File '{args.file}' non trovato")
        return
    
    analyzer = AntForagingAnalyzer(args.file)
    
    try:
        analyzer.parse_txt_file()
        analyzer.process_data()
        analyzer.calculate_temporal_normalized_efficiency()
        
        if not args.only_interactive:
            analyzer.create_matplotlib_plots()
        
        if not args.only_static:
            analyzer.create_interactive_plots()
        
        analyzer.print_summary()
        
    except Exception as e:
        print(f"Errore durante l'analisi: {e}")
        raise


if __name__ == "__main__":
    # Per test rapido senza argomenti
    import sys
    if len(sys.argv) == 1:
        print("Utilizzo: python script.py <file.txt>")
        print("Opzioni:")
        print("  --only-static      Solo grafici matplotlib")
        print("  --only-interactive Solo grafici plotly interattivi")
        sys.exit(1)
    
    main()