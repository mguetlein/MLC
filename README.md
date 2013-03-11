Martin Gütlein
05.03.2013

# MLC

project to apply multi-label-classification

## run_mlc.rb 

evaluates a multi-label-classification approach via crossvalidation, stores the results in the file mlc.results

params:  
-e endpoint-file  
-f feature-file  
-a mlc-algorithm [ECC,BR]  
-n num-endpoints [all or numeric]  
-i imputation [OFF,ON]  
-m num-missing-allowed, [all or numeric], exclusive with imputation on  
--num-cores (default: 1)  
--cv-start-seed (default: 0)  
--cv-end-seed, exclusive (default: 3)  
--classifier [SMO(default)|RandomForest]  

* output is appended to mlc.results (old mlc.results is zipped to zip/<data>mlc.results)
* the mlc.results file contains params and results.
    * params: endpoint-file, feature-file, mlc-algorithm, mlc-algorithm-params, num-enpoints, imputation-param, num-missing-allowed, classifier, classifier-params, max-num-instances, num-instances, max-num-labels, num-labels cv-seed
    * results: run-time, hamming-loss, subset-accuracy, ...


## eval_mlc.rb

compares results with different values for evaluation-param (stored in mlc.results), creates html-report with tables and figures

-e evaluation-param  
-o output-file  
--omit, omits result-row that match param-value combination, comma separated hash  
--fix, omits result-row that NOT match param-value combination, comma separated hash  

--omit-param, omits result-column, comma seperated list

example:  
compare binary relevance for different endpoint files using all endpoints
*eval_mlc.rb -e endpoint-file -o compare_endpoints_files_BR.html --fix=mlc-algorithm:ECC,num-endpoints=all*
