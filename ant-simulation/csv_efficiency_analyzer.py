import pandas as pd
import numpy as np
import sys
import math

def extract_ant_data(csv_file):
    """
    Estrae e preprocessa i dati delle formiche dal CSV
    """
    df = pd.read_csv(csv_file)
    food_events = df[df['event_type'].isin(['FOOD_PICKUP', 'FOOD_DROP'])].copy()
    food_events['ant_id'] = food_events['description'].str.extract(r'Ant (\d+)').astype(int)
    food_events = food_events.sort_values(['ant_id', 'timestamp_ns'])
    return food_events

def calculate_transport_times(ant_events):
    """
    Calcola i tempi di trasporto per ogni coppia pickup-drop
    """
    transport_data = []
    events_list = ant_events[['timestamp_ns', 'event_type', 'x', 'y']].values
    
    pickup_data = None
    for timestamp, event_type, x, y in events_list:
        if event_type == 'FOOD_PICKUP':
            pickup_data = (timestamp, x, y)
        elif event_type == 'FOOD_DROP' and pickup_data is not None:
            pickup_ts, pickup_x, pickup_y = pickup_data
            transport_time = (timestamp - pickup_ts) / 1e9  # Converti in secondi
            distance = math.sqrt((x - pickup_x)**2 + (y - pickup_y)**2)
            
            transport_data.append({
                'pickup_ts': pickup_ts,
                'drop_ts': timestamp,
                'transport_time': transport_time,
                'distance': distance,
                'pickup_x': pickup_x,
                'pickup_y': pickup_y,
                'drop_x': x,
                'drop_y': y
            })
            pickup_data = None
    
    return transport_data

def calculate_basic_stats(values, metric_name):
    """
    Calcola statistiche base per una lista di valori
    """
    if not values:
        return f"  {metric_name}: Nessun dato disponibile"
    
    values = np.array(values)
    stats = f"""  {metric_name}:
    Media: {np.mean(values):.3f}
    Mediana: {np.median(values):.3f}
    Minimo: {np.min(values):.3f}
    Massimo: {np.max(values):.3f}
    Deviazione Standard: {np.std(values):.3f}"""
    return stats

def calculate_efficiency_metrics(transport_data):
    """
    Calcola metriche di efficienza del foraging
    """
    if not transport_data:
        return "Nessun dato di trasporto disponibile"
    
    times = [t['transport_time'] for t in transport_data]
    distances = [t['distance'] for t in transport_data]
    speeds = [d/t if t > 0 else 0 for d, t in zip(distances, times)]
    
    efficiency_scores = [d/(t+1) for d, t in zip(distances, times)]  # +1 per evitare divisione per zero
    
    results = []
    results.append(calculate_basic_stats(times, "Tempi di trasporto (secondi)"))
    results.append(calculate_basic_stats(distances, "Distanze percorse"))
    results.append(calculate_basic_stats(speeds, "Velocita medie"))
    results.append(calculate_basic_stats(efficiency_scores, "Punteggi di efficienza"))
    
    return "\n".join(results)

def calculate_temporal_trends(transport_data):
    """
    Analizza le tendenze temporali dell'efficienza usando metriche relative
    """
    if len(transport_data) < 4:
        return "  Tendenze temporali: Dati insufficienti"
    
    # Calcola metriche relative per ogni viaggio
    speeds = []  # velocità = distanza / tempo
    efficiency_scores = []  # efficienza = distanza / (tempo^2) per penalizzare tempi lunghi
    timestamps = []
    
    for transport in transport_data:
        if transport['transport_time'] > 0:  # Evita divisione per zero
            speed = transport['distance'] / transport['transport_time']
            efficiency = transport['distance'] / (transport['transport_time'] ** 1.5)  # Penalizza tempi lunghi
            speeds.append(speed)
            efficiency_scores.append(efficiency)
            timestamps.append(transport['pickup_ts'])
    
    if len(speeds) < 4:
        return "  Tendenze temporali: Dati insufficienti per metriche relative"
    
    # Dividi i dati cronologicamente (non per numero sequenziale)
    total_time_span = timestamps[-1] - timestamps[0]
    mid_timestamp = timestamps[0] + total_time_span / 2
    
    # Separa in prima e seconda metà cronologica
    first_half_speeds = []
    second_half_speeds = []
    first_half_efficiency = []
    second_half_efficiency = []
    
    for i, ts in enumerate(timestamps):
        if ts <= mid_timestamp:
            first_half_speeds.append(speeds[i])
            first_half_efficiency.append(efficiency_scores[i])
        else:
            second_half_speeds.append(speeds[i])
            second_half_efficiency.append(efficiency_scores[i])
    
    # Calcola trend per velocità
    if first_half_speeds and second_half_speeds:
        avg_speed_first = np.mean(first_half_speeds)
        avg_speed_second = np.mean(second_half_speeds)
        speed_trend = ((avg_speed_second - avg_speed_first) / avg_speed_first) * 100
        
        avg_eff_first = np.mean(first_half_efficiency)
        avg_eff_second = np.mean(second_half_efficiency)
        efficiency_trend = ((avg_eff_second - avg_eff_first) / avg_eff_first) * 100
        
        speed_desc = "miglioramento" if speed_trend > 0 else "peggioramento"
        eff_desc = "miglioramento" if efficiency_trend > 0 else "peggioramento"
        
        return f"""  Tendenze temporali (metriche relative):
    Velocita media - variazione: {abs(speed_trend):.2f}% di {speed_desc}
      Prima meta cronologica: {avg_speed_first:.3f} unita/s
      Seconda meta cronologica: {avg_speed_second:.3f} unita/s
    Efficienza - variazione: {abs(efficiency_trend):.2f}% di {eff_desc}
      Prima meta cronologica: {avg_eff_first:.3f}
      Seconda meta cronologica: {avg_eff_second:.3f}"""
    
    return "  Tendenze temporali: Dati insufficienti per l'analisi cronologica"

def calculate_logarithmic_efficiency(transport_data):
    """
    Calcola l'efficienza logaritmica: distanza / (tempo * log(distanza + 1))
    """
    if not transport_data:
        return []
    
    log_efficiency = []
    for transport in transport_data:
        if transport['transport_time'] > 0 and transport['distance'] > 0:
            efficiency = transport['distance'] / (transport['transport_time'] * math.log(transport['distance'] + 1))
            log_efficiency.append(efficiency)
    
    return log_efficiency

def calculate_temporal_normalized_efficiency(all_transport_data):
    """
    Calcola l'efficienza normalizzata temporalmente per tutte le formiche
    """
    # Raccogli tutti i dati con timestamp
    all_data = []
    for ant_id, transport_data in all_transport_data.items():
        for transport in transport_data:
            if transport['transport_time'] > 0:
                speed = transport['distance'] / transport['transport_time']
                all_data.append({
                    'ant_id': ant_id,
                    'timestamp': transport['pickup_ts'],
                    'speed': speed,
                    'transport': transport
                })
    
    if not all_data:
        return {}
    
    # Ordina per timestamp
    all_data.sort(key=lambda x: x['timestamp'])
    
    # Calcola efficienza normalizzata per ogni formica
    normalized_efficiency = {}
    
    for ant_id in all_transport_data.keys():
        ant_data = [d for d in all_data if d['ant_id'] == ant_id]
        if not ant_data:
            normalized_efficiency[ant_id] = []
            continue
            
        ant_normalized = []
        for data_point in ant_data:
            # Trova finestra temporale (± 10% del tempo totale)
            total_time_span = all_data[-1]['timestamp'] - all_data[0]['timestamp']
            window_size = total_time_span * 0.1
            
            window_start = data_point['timestamp'] - window_size
            window_end = data_point['timestamp'] + window_size
            
            # Calcola velocità media di tutte le formiche nella finestra
            window_speeds = [d['speed'] for d in all_data 
                           if window_start <= d['timestamp'] <= window_end]
            
            if window_speeds:
                reference_speed = np.mean(window_speeds)
                if reference_speed > 0:
                    normalized = data_point['speed'] / reference_speed
                    ant_normalized.append(normalized)
        
        normalized_efficiency[ant_id] = ant_normalized
    
    return normalized_efficiency

def calculate_performance_consistency(transport_data):
    """
    Calcola la consistenza delle performance: 1 / coefficiente_variazione_velocità
    """
    if len(transport_data) < 2:
        return None
    
    speeds = [t['distance'] / t['transport_time'] for t in transport_data if t['transport_time'] > 0]
    
    if len(speeds) < 2:
        return None
    
    mean_speed = np.mean(speeds)
    std_speed = np.std(speeds)
    
    if mean_speed > 0:
        cv = std_speed / mean_speed  # Coefficiente di variazione
        consistency = 1 / (cv + 0.001)
        return consistency
    
    return None

def calculate_advanced_temporal_trends(transport_data):
    """
    Analizza le tendenze temporali usando le nuove metriche avanzate
    """
    if len(transport_data) < 4:
        return "  Tendenze avanzate: Dati insufficienti"
    
    # Calcola tutte le metriche
    log_efficiency = calculate_logarithmic_efficiency(transport_data)
    consistency = calculate_performance_consistency(transport_data)
    
    if not log_efficiency:
        return "  Tendenze avanzate: Dati insufficienti per calcoli"
    
    # Dividi cronologicamente per efficienza logaritmica
    timestamps = [t['pickup_ts'] for t in transport_data if t['transport_time'] > 0 and t['distance'] > 0]
    
    if len(timestamps) < len(log_efficiency):
        return "  Tendenze avanzate: Dati inconsistenti"
    
    timestamps = timestamps[:len(log_efficiency)]  # Assicura stessa lunghezza
    
    total_time_span = timestamps[-1] - timestamps[0]
    mid_timestamp = timestamps[0] + total_time_span / 2
    
    first_half_log_eff = [log_efficiency[i] for i, ts in enumerate(timestamps) if ts <= mid_timestamp]
    second_half_log_eff = [log_efficiency[i] for i, ts in enumerate(timestamps) if ts > mid_timestamp]
    
    results = []
    
    # Trend efficienza logaritmica
    if first_half_log_eff and second_half_log_eff:
        avg_log_first = np.mean(first_half_log_eff)
        avg_log_second = np.mean(second_half_log_eff)
        log_trend = ((avg_log_second - avg_log_first) / avg_log_first) * 100
        
        log_desc = "miglioramento" if log_trend > 0 else "peggioramento"
        
        results.append(f"""  Efficienza logaritmica:
    Variazione: {abs(log_trend):.2f}% di {log_desc}
    Prima meta: {avg_log_first:.4f}
    Seconda meta: {avg_log_second:.4f}""")
    
    # Consistenza performance
    if consistency is not None:
        results.append(f"""  Consistenza performance:
    Indice di consistenza: {consistency:.3f}
    (Valori alti = performance più consistenti)""")
    
    return "\n".join(results) if results else "  Tendenze avanzate: Calcoli non disponibili"

def calculate_frequency_metrics(transport_data, total_time_span):
    """
    Calcola metriche di frequenza del foraging
    """
    if not transport_data or total_time_span <= 0:
        return "  Frequenza: Dati insufficienti"
    
    num_trips = len(transport_data)
    trips_per_second = num_trips / total_time_span
    avg_interval = total_time_span / num_trips if num_trips > 0 else 0
    
    return f"""  Frequenza del foraging:
    Numero totale viaggi: {num_trips}
    Frequenza: {trips_per_second:.6f} viaggi/secondo
    Intervallo medio tra viaggi: {avg_interval:.3f} secondi"""

def extract_temporal_metrics(transport_data):
    """
    Estrae le metriche delle tendenze temporali per una formica (base + avanzate)
    """
    if len(transport_data) < 4:
        return None
    
    speeds = []
    efficiency_scores = []
    timestamps = []
    
    for transport in transport_data:
        if transport['transport_time'] > 0:
            speed = transport['distance'] / transport['transport_time']
            efficiency = transport['distance'] / (transport['transport_time'] ** 1.5)
            speeds.append(speed)
            efficiency_scores.append(efficiency)
            timestamps.append(transport['pickup_ts'])
    
    if len(speeds) < 4:
        return None
    
    # Dividi cronologicamente
    total_time_span = timestamps[-1] - timestamps[0]
    mid_timestamp = timestamps[0] + total_time_span / 2
    
    first_half_speeds = [speeds[i] for i, ts in enumerate(timestamps) if ts <= mid_timestamp]
    second_half_speeds = [speeds[i] for i, ts in enumerate(timestamps) if ts > mid_timestamp]
    first_half_efficiency = [efficiency_scores[i] for i, ts in enumerate(timestamps) if ts <= mid_timestamp]
    second_half_efficiency = [efficiency_scores[i] for i, ts in enumerate(timestamps) if ts > mid_timestamp]
    
    if not first_half_speeds or not second_half_speeds:
        return None
    
    speed_trend = ((np.mean(second_half_speeds) - np.mean(first_half_speeds)) / np.mean(first_half_speeds)) * 100
    efficiency_trend = ((np.mean(second_half_efficiency) - np.mean(first_half_efficiency)) / np.mean(first_half_efficiency)) * 100
    
    # Calcola metriche avanzate
    log_efficiency = calculate_logarithmic_efficiency(transport_data)
    consistency = calculate_performance_consistency(transport_data)
    
    # Trend efficienza logaritmica
    log_efficiency_trend = None
    if log_efficiency and len(log_efficiency) >= 4:
        valid_timestamps = [timestamps[i] for i in range(len(log_efficiency))]
        first_half_log = [log_efficiency[i] for i, ts in enumerate(valid_timestamps) if ts <= mid_timestamp]
        second_half_log = [log_efficiency[i] for i, ts in enumerate(valid_timestamps) if ts > mid_timestamp]
        
        if first_half_log and second_half_log:
            avg_log_first = np.mean(first_half_log)
            avg_log_second = np.mean(second_half_log)
            log_efficiency_trend = ((avg_log_second - avg_log_first) / avg_log_first) * 100
    
    return {
        'speed_trend': speed_trend,
        'efficiency_trend': efficiency_trend,
        'avg_speed_first': np.mean(first_half_speeds),
        'avg_speed_second': np.mean(second_half_speeds),
        'avg_efficiency_first': np.mean(first_half_efficiency),
        'avg_efficiency_second': np.mean(second_half_efficiency),
        'log_efficiency_trend': log_efficiency_trend,
        'consistency': consistency,
        'log_efficiency_scores': log_efficiency
    }

def extract_frequency_metrics(transport_data):
    """
    Estrae le metriche di frequenza per una formica
    """
    if not transport_data:
        return None
    
    first_pickup = transport_data[0]['pickup_ts']
    last_drop = transport_data[-1]['drop_ts']
    total_time_span = (last_drop - first_pickup) / 1e9
    
    if total_time_span <= 0:
        return None
    
    num_trips = len(transport_data)
    trips_per_second = num_trips / total_time_span
    avg_interval = total_time_span / num_trips
    
    return {
        'num_trips': num_trips,
        'trips_per_second': trips_per_second,
        'avg_interval': avg_interval,
        'total_time_span': total_time_span
    }

def analyze_ant_efficiency(ant_id, ant_events, use_windowing=False, generate_plots=False, window_size=1.0):
    """
    Analizza l'efficienza di una singola formica
    """
    transport_data = calculate_transport_times(ant_events)
    
    if not transport_data:
        return f"""formica {ant_id}:
  Nessun dato di trasporto completo disponibile
"""
    
    # Calcola span temporale totale
    first_pickup = transport_data[0]['pickup_ts']
    last_drop = transport_data[-1]['drop_ts']
    total_time_span = (last_drop - first_pickup) / 1e9
    
    efficiency_report = calculate_efficiency_metrics(transport_data)
    temporal_trends = calculate_temporal_trends(transport_data)
    advanced_trends = calculate_advanced_temporal_trends(transport_data)
    frequency_metrics = calculate_frequency_metrics(transport_data, total_time_span)
    
    return f"""formica {ant_id}:
{efficiency_report}

{temporal_trends}

{advanced_trends}

{frequency_metrics}

"""

def calculate_global_statistics(all_transport_data, all_temporal_metrics, all_frequency_metrics):
    """
    Calcola statistiche globali per tutte le formiche
    """
    if not all_transport_data:
        return "Nessun dato globale disponibile"
    
    # Statistiche base
    all_times = []
    all_distances = []
    all_speeds = []
    
    for ant_data in all_transport_data.values():
        for transport in ant_data:
            all_times.append(transport['transport_time'])
            all_distances.append(transport['distance'])
            if transport['transport_time'] > 0:
                all_speeds.append(transport['distance'] / transport['transport_time'])
    
    results = []
    results.append("STATISTICHE GLOBALI:")
    results.append("=" * 50)
    results.append(calculate_basic_stats(all_times, "Tempi di trasporto globali (secondi)"))
    results.append(calculate_basic_stats(all_distances, "Distanze globali"))
    results.append(calculate_basic_stats(all_speeds, "Velocita globali"))
    
    # Tendenze temporali globali (base)
    valid_temporal_data = [data for data in all_temporal_metrics.values() if data is not None]
    if valid_temporal_data:
        speed_trends = [data['speed_trend'] for data in valid_temporal_data]
        efficiency_trends = [data['efficiency_trend'] for data in valid_temporal_data]
        
        results.append(calculate_basic_stats(speed_trends, "Tendenze velocita globali (%)"))
        results.append(calculate_basic_stats(efficiency_trends, "Tendenze efficienza globali (%)"))
        
        avg_speed_improvement = np.mean(speed_trends)
        avg_efficiency_improvement = np.mean(efficiency_trends)
        
        speed_desc = "miglioramento" if avg_speed_improvement > 0 else "peggioramento"
        eff_desc = "miglioramento" if avg_efficiency_improvement > 0 else "peggioramento"
        
        results.append(f"""
  Tendenze medie complessive:
    Velocita: {abs(avg_speed_improvement):.2f}% di {speed_desc}
    Efficienza: {abs(avg_efficiency_improvement):.2f}% di {eff_desc}""")
    
    # Metriche avanzate globali
    log_efficiency_trends = [data['log_efficiency_trend'] for data in valid_temporal_data 
                           if data['log_efficiency_trend'] is not None]
    consistency_scores = [data['consistency'] for data in valid_temporal_data 
                         if data['consistency'] is not None]
    
    if log_efficiency_trends:
        results.append(calculate_basic_stats(log_efficiency_trends, "Tendenze efficienza logaritmica globali (%)"))
        
        avg_log_improvement = np.mean(log_efficiency_trends)
        log_desc = "miglioramento" if avg_log_improvement > 0 else "peggioramento"
        results.append(f"""  Efficienza logaritmica media: {abs(avg_log_improvement):.2f}% di {log_desc}""")
    
    if consistency_scores:
        results.append(calculate_basic_stats(consistency_scores, "Consistenza performance globale"))
    
    # Efficienza normalizzata temporalmente
    normalized_efficiency = calculate_temporal_normalized_efficiency(all_transport_data)
    if normalized_efficiency:
        all_normalized = []
        for ant_values in normalized_efficiency.values():
            all_normalized.extend(ant_values)
        
        if all_normalized:
            results.append(calculate_basic_stats(all_normalized, "Efficienza normalizzata temporalmente"))
    
    # Frequenza foraging globale
    valid_frequency_data = [data for data in all_frequency_metrics.values() if data is not None]
    if valid_frequency_data:
        total_trips = [data['num_trips'] for data in valid_frequency_data]
        trips_per_second = [data['trips_per_second'] for data in valid_frequency_data]
        avg_intervals = [data['avg_interval'] for data in valid_frequency_data]
        
        results.append(calculate_basic_stats(total_trips, "Viaggi per formica"))
        results.append(calculate_basic_stats(trips_per_second, "Frequenza viaggi (viaggi/secondo)"))
        results.append(calculate_basic_stats(avg_intervals, "Intervalli medi (secondi)"))
    
    # Confronto tra formiche
    ant_performances = {}
    for ant_id, transport_data in all_transport_data.items():
        if transport_data:
            avg_time = np.mean([t['transport_time'] for t in transport_data])
            ant_performances[ant_id] = avg_time
    
    if ant_performances:
        best_ant = min(ant_performances, key=ant_performances.get)
        worst_ant = max(ant_performances, key=ant_performances.get)
        results.append(f"""
  Confronto performance:
    Formica piu efficiente: {best_ant} (tempo medio: {ant_performances[best_ant]:.3f}s)
    Formica meno efficiente: {worst_ant} (tempo medio: {ant_performances[worst_ant]:.3f}s)""")
    
    return "\n".join(results)

def print_basic_events(ant_id, ant_events):
    """
    Stampa gli eventi base pickup-drop per compatibilita
    """
    transport_data = calculate_transport_times(ant_events)
    
    print(f"formica {ant_id}:")
    print()
    
    for transport in transport_data:
        print(f"pickup: {transport['pickup_ts']}")
        print(f"drop: {transport['drop_ts']}")
        print()

def analyze_ant_events(csv_file, use_windowing=False, generate_plots=False, window_size=1.0):
    """
    Funzione principale per l'analisi degli eventi delle formiche
    """
    food_events = extract_ant_data(csv_file)
    all_transport_data = {}
    all_temporal_metrics = {}
    all_frequency_metrics = {}
    
    print("ANALISI EVENTI FORAGING:")
    print("=" * 50)
    
    # Analisi per singola formica
    for ant_id in sorted(food_events['ant_id'].unique()):
        ant_events = food_events[food_events['ant_id'] == ant_id]
        print_basic_events(ant_id, ant_events)
    
    mode_description = ""
    if use_windowing:
        mode_description = f" (Analisi con finestre temporali di {window_size}s"
        if generate_plots:
            mode_description += " + generazione grafici"
        mode_description += ")"
    
    print(f"\nANALISI EFFICIENZA DETTAGLIATA{mode_description}:")
    print("=" * 50)
    
    # Analisi efficienza per singola formica e raccolta dati globali
    for ant_id in sorted(food_events['ant_id'].unique()):
        ant_events = food_events[food_events['ant_id'] == ant_id]
        transport_data = calculate_transport_times(ant_events)
        all_transport_data[ant_id] = transport_data
        
        # Raccogli metriche per statistiche globali
        all_temporal_metrics[ant_id] = extract_temporal_metrics(transport_data)
        all_frequency_metrics[ant_id] = extract_frequency_metrics(transport_data)
        
        efficiency_analysis = analyze_ant_efficiency(
            ant_id, ant_events, use_windowing=use_windowing, 
            generate_plots=generate_plots, window_size=window_size
        )
        print(efficiency_analysis)
    
    # Statistiche globali con tutte le metriche
    global_stats = calculate_global_statistics(all_transport_data, all_temporal_metrics, all_frequency_metrics)
    print(global_stats)
    
    # Riepilogo finale per modalità avanzate
    if use_windowing and generate_plots:
        print(f"\nRIEPILOGO GRAFICI GENERATI:")
        print("=" * 50)
        ant_count = len(food_events['ant_id'].unique())
        print(f"Grafici generati per {ant_count} formiche con finestre di {window_size} secondi")
        print("File salvati: trends_formica_[ID].png")
        print("Ogni grafico contiene: velocita, efficienza logaritmica, consistenza, confronto normalizzato")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python script.py <file.csv> [--windowing] [--plots] [--window-size=N]")
        print("\nOpzioni:")
        print("  --windowing     Attiva analisi con finestre temporali")
        print("  --plots         Genera grafici delle tendenze (richiede --windowing)")
        print("  --window-size=N Imposta dimensione finestra in secondi (default: 1.0)")
        print("\nEsempi:")
        print("  python script.py data.csv")
        print("  python script.py data.csv --windowing")
        print("  python script.py data.csv --windowing --plots")
        print("  python script.py data.csv --windowing --plots --window-size=0.5")
        sys.exit(1)
    
    csv_file = sys.argv[1]
    use_windowing = '--windowing' in sys.argv
    generate_plots = '--plots' in sys.argv
    window_size = 1.0
    
    # Parsing della dimensione della finestra
    for arg in sys.argv:
        if arg.startswith('--window-size='):
            try:
                window_size = float(arg.split('=')[1])
                if window_size <= 0:
                    print("Errore: La dimensione della finestra deve essere positiva")
                    sys.exit(1)
            except ValueError:
                print("Errore: Valore non valido per window-size")
                sys.exit(1)
    
    # Verifica dipendenze per i grafici
    if generate_plots and not use_windowing:
        print("Errore: --plots richiede --windowing")
        sys.exit(1)
    
    if generate_plots:
        try:
            import matplotlib.pyplot as plt
        except ImportError:
            print("Errore: matplotlib richiesto per la generazione dei grafici")
            print("Installa con: pip install matplotlib")
            sys.exit(1)
    
    try:
        analyze_ant_events(csv_file, use_windowing, generate_plots, window_size)
    except FileNotFoundError:
        print(f"Errore: File '{csv_file}' non trovato")
        sys.exit(1)
    except Exception as e:
        print(f"Errore durante l'analisi: {e}")
        sys.exit(1)