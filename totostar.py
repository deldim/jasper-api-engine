
import os
import time
import gc
import getStandardizedInput
import splitInputForPricer
import getStandardizedScenario
import instrumentPricer
import pandas as pd
from concurrent.futures import ProcessPoolExecutor

def makefolders(root_dir, subfolders):
    for subfolder in subfolders:
        os.makedirs(os.path.join(root_dir, subfolder), exist_ok=True)

def run_pricing(valoDate, instr_table):
    try:
        instrumentPricer.main(valoDate, instr_table)
    except Exception as e:
        print(f"Pricing failed for {instr_table}: {e}")
    finally:
        gc.collect()

def main():
    root_dir = r"C:\Users\c34387\OneDrive - BNP Paribas\wow\DataFactory\TestingFolder"
    subfolders = [
        "StandardizationResults/Instruments", 
        "StandardizationResults/Scenarios", 
        "PricingResults",
        "AggregationResults"
    ]
    valoDate = "20250131"

    start = time.time()
    makefolders(root_dir, subfolders)
    print("Folder creation done")

    # STEP 1: Instrument Standardization
    t1 = time.time()
    df_standardized_input_data = getStandardizedInput.main()
    print("Step 1 done in {:.2f}s".format(time.time() - t1))

    # STEP 2: Split for pricing
    t2 = time.time()
    list_input_tables = splitInputForPricer.main(df_standardized_input_data)
    del df_standardized_input_data
    gc.collect()
    print("Step 2 done in {:.2f}s".format(time.time() - t2))

    # STEP 3: Scenario prep
    t3 = time.time()
    getStandardizedScenario.main()
    gc.collect()
    print("Step 3 done in {:.2f}s".format(time.time() - t3))

    # STEP 4: Pricing (Parallelized)
    t4 = time.time()
    with ProcessPoolExecutor(max_workers=4) as executor:
        futures = [executor.submit(run_pricing, valoDate, table) for table in list_input_tables]
        for f in futures:
            f.result()
    print("Step 4 done in {:.2f}s".format(time.time() - t4))

    print("Total runtime: {:.2f}s".format(time.time() - start))

if __name__ == "__main__":
    main()

