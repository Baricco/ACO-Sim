import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.animation as animation
from matplotlib.colors import LinearSegmentedColormap
import seaborn as sns
from scipy.spatial.distance import cdist
import warnings
from pathlib import Path
import sys

warnings.filterwarnings('ignore')

class TrailStabilizationAnalyzer:
    def __init__(self, csv_path):
        self.csv_path = csv_path
        self.MAP_WIDTH = 800
        self.MAP_HEIGHT = 600
        self.TRAIL_DISTANCE = 50  # pixel distance to consider same trail
        self.GRID_SIZE = 25       # size of grid cells for heat map
        self.TIME_WINDOW = 1    # seconds
        
        # Calculate grid dimensions
        self.grid_cols = int(self.MAP_WIDTH / self.GRID_SIZE)
        self.grid_rows = int(self.MAP_HEIGHT / self.GRID_SIZE)
        
        self.df = self.load_and_preprocess_data()
        
    def load_and_preprocess_data(self):
        """Load and preprocess CSV data"""
        try:
            df = pd.read_csv(self.csv_path)
            
            # Convert timestamps to seconds
            df['time_sec'] = df['timestamp_ns'] / 1e9
            df['time_relative'] = df['time_sec'] - df['time_sec'].min()
            
            # Extract ant IDs
            df['ant_id'] = df['description'].str.extract(r'Ant (\d+)', expand=False)
            df['ant_id'] = pd.to_numeric(df['ant_id'], errors='coerce')
            
            # Filter valid coordinates
            df = df.dropna(subset=['x', 'y'])
            df = df[(df['x'] >= 0) & (df['x'] <= self.MAP_WIDTH) & 
                   (df['y'] >= 0) & (df['y'] <= self.MAP_HEIGHT)]
            
            print(f"Loaded {len(df)} records over {df['time_relative'].max():.1f} seconds")
            return df
            
        except Exception as e:
            print(f"Error loading CSV: {e}")
            return pd.DataFrame()
    
    def print_section_header(self, title):
        """Print formatted section header"""
        print("\n" + "=" * 70)
        print(f" {title.upper()}")
        print("=" * 70)
    
    def approach_1_spatial_entropy(self):
        """Approach 1: Heat Map Temporal Analysis"""
        self.print_section_header("APPROCCIO 1: ANALISI ENTROPIA SPAZIALE")
        
        positions = self.df[self.df['event_type'] == 'ANT_POSITION'].copy()
        if positions.empty:
            print("No position data available")
            return
        
        # Calculate grid coordinates
        positions['grid_x'] = np.floor(positions['x'] / self.GRID_SIZE).astype(int)
        positions['grid_y'] = np.floor(positions['y'] / self.GRID_SIZE).astype(int)
        positions['time_bin'] = np.floor(positions['time_relative'] / self.TIME_WINDOW).astype(int)
        
        # Calculate entropy over time
        time_bins = sorted(positions['time_bin'].unique())
        entropies = []
        total_visits = []
        max_cell_visits = []
        
        heat_maps = []  # Store heat maps for animation
        
        for t_bin in time_bins:
            bin_data = positions[positions['time_bin'] == t_bin]
            
            if len(bin_data) == 0:
                entropies.append(np.nan)
                total_visits.append(0)
                max_cell_visits.append(0)
                heat_maps.append(np.zeros((self.grid_rows, self.grid_cols)))
                continue
            
            # Create heat map
            heat_map = np.zeros((self.grid_rows, self.grid_cols))
            visit_counts = bin_data.groupby(['grid_y', 'grid_x']).size()
            
            for (gy, gx), count in visit_counts.items():
                if 0 <= gy < self.grid_rows and 0 <= gx < self.grid_cols:
                    heat_map[gy, gx] = count
            
            heat_maps.append(heat_map)
            
            # Calculate entropy
            total_visits_bin = len(bin_data)
            if total_visits_bin > 0:
                probabilities = visit_counts.values / total_visits_bin
                entropy = -np.sum(probabilities * np.log2(probabilities + 1e-10))
                entropies.append(entropy)
                total_visits.append(total_visits_bin)
                max_cell_visits.append(visit_counts.max())
            else:
                entropies.append(np.nan)
                total_visits.append(0)
                max_cell_visits.append(0)
        
        # Calculate statistics
        valid_entropies = [e for e in entropies if not np.isnan(e)]
        if valid_entropies:
            initial_entropy = np.mean(valid_entropies[:5]) if len(valid_entropies) >= 5 else valid_entropies[0]
            final_entropy = np.mean(valid_entropies[-5:]) if len(valid_entropies) >= 5 else valid_entropies[-1]
            entropy_reduction = ((initial_entropy - final_entropy) / initial_entropy * 100) if initial_entropy > 0 else 0
            
            print(f"  Entropia iniziale (primi 5s):      {initial_entropy:.3f} bit")
            print(f"  Entropia finale (ultimi 5s):       {final_entropy:.3f} bit")
            print(f"  Riduzione entropia:                 {entropy_reduction:.1f}%")
            print(f"  Concentrazione massima per cella:   {max(max_cell_visits)} visite")
            print(f"  Deviazione standard entropia:       {np.std(valid_entropies):.3f}")
        
        # Create entropy plot
        plt.figure(figsize=(12, 8))
        
        plt.subplot(2, 2, 1)
        times = [t * self.TIME_WINDOW for t in time_bins]
        plt.plot(times, entropies, 'b-', linewidth=2, alpha=0.8)
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Entropia Spaziale (bit)')
        plt.title('Evoluzione dell\'Entropia Spaziale')
        plt.grid(True, alpha=0.3)
        
        plt.subplot(2, 2, 2)
        plt.plot(times, total_visits, 'g-', linewidth=2, alpha=0.8)
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Visite Totali per Secondo')
        plt.title('Intensità di Movimento')
        plt.grid(True, alpha=0.3)
        
        plt.subplot(2, 2, 3)
        plt.plot(times, max_cell_visits, 'r-', linewidth=2, alpha=0.8)
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Massime Visite per Cella')
        plt.title('Concentrazione Massima')
        plt.grid(True, alpha=0.3)
        
        # Final heat map
        plt.subplot(2, 2, 4)
        if heat_maps:
            final_heatmap = np.sum(heat_maps[-10:], axis=0) if len(heat_maps) >= 10 else heat_maps[-1]
            im = plt.imshow(final_heatmap, cmap='hot', interpolation='nearest', 
                           extent=[0, self.MAP_WIDTH, self.MAP_HEIGHT, 0])
            plt.colorbar(im, label='Visite')
            plt.xlabel('X (pixel)')
            plt.ylabel('Y (pixel)')
            plt.title('Heat Map Finale (ultimi 10s)')
        
        plt.tight_layout()
        plt.show()
        
        return {
            'entropies': entropies,
            'times': times,
            'heat_maps': heat_maps,
            'entropy_reduction': entropy_reduction if valid_entropies else 0
        }
    
    def approach_2_beaten_paths(self):
        """Approach 2: Beaten Paths vs Virgin Exploration"""
        self.print_section_header("APPROCCIO 2: SENTIERI BATTUTI vs ESPLORAZIONE VERGINE")
        
        positions = self.df[self.df['event_type'] == 'ANT_POSITION'].copy()
        if positions.empty:
            print("No position data available")
            return
        
        positions = positions.sort_values('time_relative')
        positions['time_bin'] = np.floor(positions['time_relative'] / self.TIME_WINDOW).astype(int)
        
        time_bins = sorted(positions['time_bin'].unique())
        explored_positions = []
        beaten_path_percentages = []
        virgin_exploration_percentages = []
        cumulative_exploration_area = []
        
        for t_bin in time_bins:
            current_positions = positions[positions['time_bin'] == t_bin][['x', 'y']].values
            
            if len(current_positions) == 0:
                beaten_path_percentages.append(0)
                virgin_exploration_percentages.append(0)
                cumulative_exploration_area.append(len(explored_positions) * np.pi * self.TRAIL_DISTANCE**2)
                continue
            
            beaten_count = 0
            virgin_count = 0
            
            for pos in current_positions:
                if len(explored_positions) == 0:
                    # First positions are always virgin exploration
                    virgin_count += 1
                    explored_positions.append(pos)
                else:
                    # Check distance to all previously explored positions
                    distances = cdist([pos], explored_positions, metric='euclidean')[0]
                    min_distance = np.min(distances)
                    
                    if min_distance <= self.TRAIL_DISTANCE:
                        beaten_count += 1
                    else:
                        virgin_count += 1
                        explored_positions.append(pos)
            
            total_moves = beaten_count + virgin_count
            if total_moves > 0:
                beaten_path_percentages.append((beaten_count / total_moves) * 100)
                virgin_exploration_percentages.append((virgin_count / total_moves) * 100)
            else:
                beaten_path_percentages.append(0)
                virgin_exploration_percentages.append(0)
            
            # Approximate exploration area (overlapping circles)
            area = len(explored_positions) * np.pi * (self.TRAIL_DISTANCE/2)**2
            cumulative_exploration_area.append(area)
        
        # Calculate statistics
        if beaten_path_percentages:
            initial_beaten = np.mean(beaten_path_percentages[:10]) if len(beaten_path_percentages) >= 10 else beaten_path_percentages[0] if beaten_path_percentages else 0
            final_beaten = np.mean(beaten_path_percentages[-10:]) if len(beaten_path_percentages) >= 10 else beaten_path_percentages[-1] if beaten_path_percentages else 0
            max_beaten = max(beaten_path_percentages) if beaten_path_percentages else 0
            
            print(f"  Sentieri battuti iniziali (primi 10s): {initial_beaten:.1f}%")
            print(f"  Sentieri battuti finali (ultimi 10s):  {final_beaten:.1f}%")
            print(f"  Picco massimo sentieri battuti:        {max_beaten:.1f}%")
            print(f"  Incremento uso sentieri battuti:       {final_beaten - initial_beaten:.1f} punti percentuali")
            print(f"  Posizioni uniche esplorate:            {len(explored_positions)}")
            print(f"  Area di esplorazione finale:           {cumulative_exploration_area[-1] if cumulative_exploration_area else 0:.0f} pixel²")
            
            # Calculate convergence point (when beaten paths > 50%)
            convergence_time = None
            for i, pct in enumerate(beaten_path_percentages):
                if pct > 50:
                    convergence_time = i * self.TIME_WINDOW
                    break
            
            if convergence_time:
                print(f"  Tempo di convergenza (>50% battuti):   {convergence_time:.1f} secondi")
        
        # Create plots
        plt.figure(figsize=(15, 10))
        
        times = [t * self.TIME_WINDOW for t in time_bins]
        
        plt.subplot(2, 3, 1)
        plt.plot(times, beaten_path_percentages, 'b-', linewidth=2, alpha=0.8, label='Sentieri Battuti')
        plt.plot(times, virgin_exploration_percentages, 'r-', linewidth=2, alpha=0.8, label='Esplorazione Vergine')
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Percentuale Movimenti (%)')
        plt.title('Sentieri Battuti vs Esplorazione Vergine')
        plt.legend()
        plt.grid(True, alpha=0.3)
        
        plt.subplot(2, 3, 2)
        plt.fill_between(times, 0, beaten_path_percentages, alpha=0.3, color='blue', label='Sentieri Battuti')
        plt.fill_between(times, beaten_path_percentages, 100, alpha=0.3, color='red', label='Esplorazione Vergine')
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Percentuale Movimenti (%)')
        plt.title('Composizione Movimenti (Area)')
        plt.legend()
        plt.grid(True, alpha=0.3)
        
        plt.subplot(2, 3, 3)
        cumulative_positions = np.arange(1, len(time_bins) + 1) * np.mean([len(positions[positions['time_bin'] == t]) for t in time_bins[:10]]) if time_bins else []
        if cumulative_positions:
            exploration_efficiency = [len(explored_positions[:i+1]) / cum_pos * 100 if cum_pos > 0 else 0 
                                    for i, cum_pos in enumerate(cumulative_positions)]
            plt.plot(times[:len(exploration_efficiency)], exploration_efficiency, 'g-', linewidth=2, alpha=0.8)
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Efficienza Esplorazione (%)')
        plt.title('Efficienza di Esplorazione')
        plt.grid(True, alpha=0.3)
        
        # Visualization of exploration map
        plt.subplot(2, 3, 4)
        if explored_positions:
            explored_array = np.array(explored_positions)
            plt.scatter(explored_array[:, 0], explored_array[:, 1], 
                       c=range(len(explored_array)), cmap='viridis', 
                       alpha=0.6, s=20)
            plt.colorbar(label='Ordine di Scoperta')
            plt.xlim(0, self.MAP_WIDTH)
            plt.ylim(0, self.MAP_HEIGHT)
            plt.xlabel('X (pixel)')
            plt.ylabel('Y (pixel)')
            plt.title('Mappa Posizioni Esplorate')
        
        plt.subplot(2, 3, 5)
        plt.plot(times, cumulative_exploration_area, 'm-', linewidth=2, alpha=0.8)
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Area Esplorata (pixel²)')
        plt.title('Crescita Area di Esplorazione')
        plt.grid(True, alpha=0.3)
        
        # Rolling average of beaten paths
        plt.subplot(2, 3, 6)
        if len(beaten_path_percentages) > 5:
            window_size = min(10, len(beaten_path_percentages) // 4)
            rolling_beaten = pd.Series(beaten_path_percentages).rolling(window_size, center=True).mean()
            plt.plot(times, beaten_path_percentages, 'b-', alpha=0.3, label='Dati Grezzi')
            plt.plot(times, rolling_beaten, 'b-', linewidth=3, label=f'Media Mobile ({window_size}s)')
            plt.xlabel('Tempo (secondi)')
            plt.ylabel('% Sentieri Battuti')
            plt.title('Trend Stabilizzazione Sentieri')
            plt.legend()
            plt.grid(True, alpha=0.3)
        
        plt.tight_layout()
        plt.show()
        
        return {
            'beaten_percentages': beaten_path_percentages,
            'virgin_percentages': virgin_exploration_percentages,
            'times': times,
            'explored_positions': explored_positions,
            'convergence_time': convergence_time if 'convergence_time' in locals() else None
        }
    
    def approach_3_pheromone_effectiveness(self):
        """Approach 3: Pheromone Usage Evolution"""
        self.print_section_header("APPROCCIO 3: EVOLUZIONE EFFICACIA FEROMONALE")
        
        decisions = self.df[self.df['event_type'] == 'ANT_DECISION'].copy()
        if decisions.empty:
            print("No decision data available")
            return
        
        # Parse decision data
        decisions['using_pheromones'] = decisions['data'].str.contains('using_pheromones=true', na=False)
        decisions['pheromone_intensity'] = decisions['data'].str.extract(r'pheromone_intensity=([^;]+)', expand=False)
        decisions['pheromone_intensity'] = pd.to_numeric(decisions['pheromone_intensity'], errors='coerce').fillna(0)
        
        # Extract decision types
        decisions['decision_type'] = decisions['description'].str.extract(r'- (\w+)', expand=False)
        
        decisions['time_bin'] = np.floor(decisions['time_relative'] / self.TIME_WINDOW).astype(int)
        
        time_bins = sorted(decisions['time_bin'].unique())
        
        pheromone_usage_pct = []
        avg_pheromone_intensity = []
        food_pheromone_pct = []
        nest_pheromone_pct = []
        random_decisions_pct = []
        total_decisions = []
        
        for t_bin in time_bins:
            bin_data = decisions[decisions['time_bin'] == t_bin]
            
            if len(bin_data) == 0:
                pheromone_usage_pct.append(0)
                avg_pheromone_intensity.append(0)
                food_pheromone_pct.append(0)
                nest_pheromone_pct.append(0)
                random_decisions_pct.append(0)
                total_decisions.append(0)
                continue
            
            total = len(bin_data)
            total_decisions.append(total)
            
            # Pheromone usage percentage
            pheromone_count = bin_data['using_pheromones'].sum()
            pheromone_usage_pct.append((pheromone_count / total) * 100)
            
            # Average pheromone intensity
            pheromone_data = bin_data[bin_data['using_pheromones']]
            if len(pheromone_data) > 0:
                avg_pheromone_intensity.append(pheromone_data['pheromone_intensity'].mean())
            else:
                avg_pheromone_intensity.append(0)
            
            # Decision type percentages
            decision_counts = bin_data['decision_type'].value_counts()
            
            food_pheromone_pct.append((decision_counts.get('FOLLOW_FOOD_PHEROMONE', 0) / total) * 100)
            nest_pheromone_pct.append((decision_counts.get('FOLLOW_NEST_PHEROMONE', 0) / total) * 100)
            random_decisions_pct.append((decision_counts.get('RANDOM_WALK', 0) / total) * 100)
        
        # Calculate statistics
        if pheromone_usage_pct:
            initial_pheromone_use = np.mean(pheromone_usage_pct[:10]) if len(pheromone_usage_pct) >= 10 else pheromone_usage_pct[0]
            final_pheromone_use = np.mean(pheromone_usage_pct[-10:]) if len(pheromone_usage_pct) >= 10 else pheromone_usage_pct[-1]
            
            initial_random = np.mean(random_decisions_pct[:10]) if len(random_decisions_pct) >= 10 else random_decisions_pct[0]
            final_random = np.mean(random_decisions_pct[-10:]) if len(random_decisions_pct) >= 10 else random_decisions_pct[-1]
            
            peak_intensity = max(avg_pheromone_intensity) if avg_pheromone_intensity else 0
            
            print(f"  Uso feromoni iniziale (primi 10s):     {initial_pheromone_use:.1f}%")
            print(f"  Uso feromoni finale (ultimi 10s):      {final_pheromone_use:.1f}%")
            print(f"  Incremento uso feromoni:               {final_pheromone_use - initial_pheromone_use:.1f} punti percentuali")
            print(f"  Intensità feromonale media finale:     {np.mean(avg_pheromone_intensity[-10:]) if len(avg_pheromone_intensity) >= 10 else avg_pheromone_intensity[-1] if avg_pheromone_intensity else 0:.3f}")
            print(f"  Picco intensità feromonale:            {peak_intensity:.3f}")
            print(f"  Decisioni casuali iniziali:            {initial_random:.1f}%")
            print(f"  Decisioni casuali finali:              {final_random:.1f}%")
            print(f"  Riduzione comportamenti casuali:       {initial_random - final_random:.1f} punti percentuali")
        
        # Create plots
        plt.figure(figsize=(15, 12))
        
        times = [t * self.TIME_WINDOW for t in time_bins]
        
        plt.subplot(3, 3, 1)
        plt.plot(times, pheromone_usage_pct, 'b-', linewidth=2, alpha=0.8)
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Uso Feromoni (%)')
        plt.title('Evoluzione Uso Feromoni')
        plt.grid(True, alpha=0.3)
        
        plt.subplot(3, 3, 2)
        plt.plot(times, avg_pheromone_intensity, 'g-', linewidth=2, alpha=0.8)
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Intensità Media')
        plt.title('Intensità Feromonale Media')
        plt.grid(True, alpha=0.3)
        
        plt.subplot(3, 3, 3)
        plt.plot(times, random_decisions_pct, 'r-', linewidth=2, alpha=0.8)
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Decisioni Casuali (%)')
        plt.title('Riduzione Comportamenti Casuali')
        plt.grid(True, alpha=0.3)
        
        plt.subplot(3, 3, 4)
        plt.plot(times, food_pheromone_pct, 'orange', linewidth=2, alpha=0.8, label='Feromoni Cibo')
        plt.plot(times, nest_pheromone_pct, 'brown', linewidth=2, alpha=0.8, label='Feromoni Nido')
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Tipo Decisioni (%)')
        plt.title('Distribuzione Tipi di Feromoni')
        plt.legend()
        plt.grid(True, alpha=0.3)
        
        plt.subplot(3, 3, 5)
        plt.fill_between(times, 0, food_pheromone_pct, alpha=0.3, color='orange', label='Feromoni Cibo')
        plt.fill_between(times, food_pheromone_pct, 
                        np.array(food_pheromone_pct) + np.array(nest_pheromone_pct), 
                        alpha=0.3, color='brown', label='Feromoni Nido')
        plt.fill_between(times, np.array(food_pheromone_pct) + np.array(nest_pheromone_pct), 
                        100, alpha=0.3, color='red', label='Altri')
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Composizione Decisioni (%)')
        plt.title('Composizione Decisionale')
        plt.legend()
        plt.grid(True, alpha=0.3)
        
        plt.subplot(3, 3, 6)
        plt.plot(times, total_decisions, 'm-', linewidth=2, alpha=0.8)
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('N. Decisioni')
        plt.title('Attività Decisionale')
        plt.grid(True, alpha=0.3)
        
        # Correlation between intensity and usage
        plt.subplot(3, 3, 7)
        if len(pheromone_usage_pct) == len(avg_pheromone_intensity):
            plt.scatter(pheromone_usage_pct, avg_pheromone_intensity, alpha=0.6, c=times, cmap='viridis')
            plt.colorbar(label='Tempo (s)')
            plt.xlabel('Uso Feromoni (%)')
            plt.ylabel('Intensità Media')
            plt.title('Correlazione Uso-Intensità')
            plt.grid(True, alpha=0.3)
        
        # Efficiency metric: pheromone use / random decisions ratio
        plt.subplot(3, 3, 8)
        efficiency_ratio = []
        for i in range(len(pheromone_usage_pct)):
            if random_decisions_pct[i] > 0:
                efficiency_ratio.append(pheromone_usage_pct[i] / random_decisions_pct[i])
            else:
                efficiency_ratio.append(pheromone_usage_pct[i] if pheromone_usage_pct[i] > 0 else 0)
        
        plt.plot(times, efficiency_ratio, 'purple', linewidth=2, alpha=0.8)
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Rapporto Feromoni/Casuali')
        plt.title('Indice di Strutturazione')
        plt.grid(True, alpha=0.3)
        
        # Decision diversity (entropy)
        plt.subplot(3, 3, 9)
        decision_entropy = []
        for t_bin in time_bins:
            bin_data = decisions[decisions['time_bin'] == t_bin]
            if len(bin_data) > 0:
                decision_counts = bin_data['decision_type'].value_counts()
                probabilities = decision_counts.values / decision_counts.sum()
                entropy = -np.sum(probabilities * np.log2(probabilities + 1e-10))
                decision_entropy.append(entropy)
            else:
                decision_entropy.append(0)
        
        plt.plot(times, decision_entropy, 'teal', linewidth=2, alpha=0.8)
        plt.xlabel('Tempo (secondi)')
        plt.ylabel('Entropia Decisionale (bit)')
        plt.title('Diversità Comportamentale')
        plt.grid(True, alpha=0.3)
        
        plt.tight_layout()
        plt.show()
        
        return {
            'pheromone_usage': pheromone_usage_pct,
            'pheromone_intensity': avg_pheromone_intensity,
            'times': times,
            'efficiency_ratio': efficiency_ratio,
            'decision_entropy': decision_entropy
        }
    
    def generate_summary_report(self, results1, results2, results3):
        """Generate comprehensive summary report"""
        self.print_section_header("REPORT RIASSUNTIVO: STABILIZZAZIONE DEI SENTIERI")
        
        print("\n📊 INDICATORI CHIAVE DI STABILIZZAZIONE:")
        print("-" * 50)
        
        if results1 and 'entropy_reduction' in results1:
            print(f"  🔍 Riduzione entropia spaziale:        {results1['entropy_reduction']:.1f}%")
        
        if results2 and results2.get('beaten_percentages'):
            final_beaten = results2['beaten_percentages'][-1] if results2['beaten_percentages'] else 0
            print(f"  🛤️  Utilizzo sentieri battuti finale:  {final_beaten:.1f}%")
            
            if results2.get('convergence_time'):
                print(f"  ⏱️  Tempo di convergenza:              {results2['convergence_time']:.1f} secondi")
        
        if results3 and results3.get('pheromone_usage'):
            final_pheromone = results3['pheromone_usage'][-1] if results3['pheromone_usage'] else 0
            print(f"  🧪 Uso feromoni finale:                {final_pheromone:.1f}%")
        
        print("\n📈 EVIDENZE DI EMERGENZA:")
        print("-" * 50)
        
        stabilization_score = 0
        max_score = 100
        
        # Entropy reduction contributes up to 30 points
        if results1 and 'entropy_reduction' in results1:
            entropy_score = min(30, results1['entropy_reduction'] * 0.5)
            stabilization_score += entropy_score
            print(f"  • Concentrazione spaziale (max 30pts): {entropy_score:.1f} punti")
        
        # Trail usage contributes up to 40 points
        if results2 and results2.get('beaten_percentages'):
            trail_score = min(40, max(results2['beaten_percentages']) * 0.4)
            stabilization_score += trail_score
            print(f"  • Riutilizzo percorsi (max 40pts):     {trail_score:.1f} punti")
        
        # Pheromone effectiveness contributes up to 30 points
        if results3 and results3.get('pheromone_usage'):
            pheromone_score = min(30, max(results3['pheromone_usage']) * 0.3)
            stabilization_score += pheromone_score
            print(f"  • Efficacia feromonale (max 30pts):    {pheromone_score:.1f} punti")
        
        print(f"\n🎯 PUNTEGGIO STABILIZZAZIONE COMPLESSIVO: {stabilization_score:.1f}/{max_score}")
        
        if stabilization_score >= 80:
            assessment = "ELEVATA - Sentieri altamente stabilizzati"
        elif stabilization_score >= 60:
            assessment = "BUONA - Chiara formazione di sentieri"
        elif stabilization_score >= 40:
            assessment = "MODERATA - Emergenza parziale di pattern"
        else:
            assessment = "BASSA - Comportamento prevalentemente esplorativo"
        
        print(f"📊 VALUTAZIONE: {assessment}")
        
        print("\n" + "=" * 70)
    
    def run_complete_analysis(self):
        """Run all three approaches and generate comprehensive report"""
        self.print_section_header("ANALISI COMPLETA STABILIZZAZIONE SENTIERI")
        
        if self.df.empty:
            print("❌ Impossibile procedere: dati CSV non validi")
            return
        
        print(f"📁 File: {self.csv_path}")
        print(f"📊 Records: {len(self.df):,}")
        print(f"⏱️  Durata: {self.df['time_relative'].max():.1f} secondi")
        print(f"🐜 Formiche: {self.df['ant_id'].nunique()}")
        print(f"🗺️  Mappa: {self.MAP_WIDTH}x{self.MAP_HEIGHT} pixel")
        print(f"📏 Soglia sentiero: {self.TRAIL_DISTANCE} pixel")
        print(f"⏰ Finestra temporale: {self.TIME_WINDOW} secondo/i")
        
        # Run all three approaches
        try:
            results1 = self.approach_1_spatial_entropy()
        except Exception as e:
            print(f"⚠️ Errore Approccio 1: {e}")
            results1 = None
        
        try:
            results2 = self.approach_2_beaten_paths()
        except Exception as e:
            print(f"⚠️ Errore Approccio 2: {e}")
            results2 = None
        
        try:
            results3 = self.approach_3_pheromone_effectiveness()
        except Exception as e:
            print(f"⚠️ Errore Approccio 3: {e}")
            results3 = None
        
        # Generate summary report
        self.generate_summary_report(results1, results2, results3)
        
        return {
            'spatial_entropy': results1,
            'beaten_paths': results2,
            'pheromone_effectiveness': results3
        }

def main():
    if len(sys.argv) != 2:
        print("Uso: python csv_advanced_analyzer.py <file.csv>")
        print("Esempio: python csv_advanced_analyzer.py experiment_full_sim.csv")
        sys.exit(1)
    
    csv_file = sys.argv[1]
    if not Path(csv_file).exists():
        print(f"❌ Errore: File '{csv_file}' non trovato")
        sys.exit(1)
    
    print("🐜 ANALIZZATORE AVANZATO STABILIZZAZIONE SENTIERI")
    print("=" * 60)
    
    analyzer = TrailStabilizationAnalyzer(csv_file)
    results = analyzer.run_complete_analysis()
    
    print("\n✅ Analisi completata!")
    print("📊 Grafici visualizzati - Chiudere le finestre per terminare.")

if __name__ == "__main__":
    main()