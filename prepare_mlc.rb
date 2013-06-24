require "rubygems"
require "getoptlong"

require "./to_arff.rb"

opts = GetoptLong.new(
  [ '--dataset-name', '-d', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--endpoint-file', '-e', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--real-endpoint-file', '-r', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--feature-file', '-f', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--num-endpoints', '-n', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--num-missing-allowed', '-m', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--start-endpoint', '-i', GetoptLong::REQUIRED_ARGUMENT ],
)

usage = "Usage: ..."

endpoint_file = nil
real_endpoint_file = nil
feature_file = nil
num_endpoints = nil
start_endpoint = 0
num_missing_allowed = nil
dataset_name = nil

#num_cores = nil
#min_cv_seed = 0
#max_cv_seed_exclusive = 3
#mlc_algorithm = "ECC"

#run_eval = false

opts.each do |opt, arg|
  case opt
  when '--endpoint-file'
    endpoint_file = arg
  when '--real-endpoint-file'
    real_endpoint_file = arg
  when '--feature-file'
    feature_file = arg
  when '--num-endpoints'
    num_endpoints = arg
  when '--num-missing-allowed'
    num_missing_allowed = arg
  when '--dataset-name'
    dataset_name = arg
  when '--start-endpoint'
    start_endpoint = arg.to_i
  end
#  when '--num-cores'
#    num_cores = arg
#    run_eval = true
#  when '--min-cv-seed'
#    min_cv_seed = arg.to_i
#    run_eval = true
#  when '--max-cv-seed-exclusive'
#    max_cv_seed_exclusive = arg
#    run_eval = true
#  when '--mlc-algorithm'
#    mlc_algorithm = arg
#    run_eval = true
#  end  
end

raise "dataset-name missing\n"+usage unless dataset_name

raise "enpoint-file missing\n"+usage unless endpoint_file
raise "enpoint-file missing #{endpoint_file}" unless File.exist? endpoint_file
puts "Endpoint file: "+endpoint_file

discretization = 2
#if endpoint_file=~/_disc2/ or endpoint_file=~/_discV2/
#  discretization = 2
#elsif endpoint_file=~/_clust[0-9][0-9]to[0-9][0-9]/
#  discretization = 2
#elsif endpoint_file=~/CPDB/
#  discretization = 2
#else
#  raise "cannot determine discretization"  
#end

if endpoint_file=~/_withV/
  include_v = true
else
  include_v = false
end

raise "feature-file missing\n"+usage unless feature_file
raise "could not find #{feature_file}" unless File.exist? feature_file
puts "Feature file: "+feature_file

raise "num-endpoints\n"+usage unless num_endpoints
puts "Num endpoints: #{num_endpoints==-1 ? "all" : num_endpoints}"

raise "num-missing-allowed\n"+usage unless num_missing_allowed
puts "Num missing allowed: #{num_missing_allowed==-1 ? "all" : num_missing_allowed}"


class_map = nil
if(endpoint_file =~ /_noV_EqF/)
  class_map = {"0" => "high-real-value(>50)", "1" => "low-real-value(<50)", "missing" => "unknown_or_fully-tested(V)"}
elsif(endpoint_file =~ /_withV_RvsV/)
  class_map = {"0" => "fully-tested(V)", "1" => "real-value", "missing" => "unknown"}
elsif(endpoint_file =~ /_withV_Cl([0-9])([0-9])V/)
  low_pct = $1.to_i*10
  high_pct = $2.to_i*10
  mid_pct = low_pct + (high_pct-low_pct)/2
  class_map = {"0" => "high-real-value(>~#{mid_pct})_or_fully-tested(V)", "1" => "low-real-value(<~#{mid_pct})", "missing" => "unknown"}  
elsif(endpoint_file =~ /_noV_Cl([0-9])([0-9])/)
  low_pct = $1.to_i*10
  high_pct = $2.to_i*10
  mid_pct = low_pct + (high_pct-low_pct)/2 
  class_map = {"0" => "high-real-value(>~#{mid_pct})", "1" => "low-real-value(<~#{mid_pct})", "missing" => "unknown_or_fully-tested(V)"}
end
raise "please specifiy class-value-map" unless class_map


toArff = ToArff.new(endpoint_file, feature_file, real_endpoint_file)
num_endpoints = num_endpoints=="all" ? toArff.num_max_endpoints : num_endpoints.to_i
num_missing_allowed = num_missing_allowed=="all" ? num_endpoints : num_missing_allowed.to_i
relation_name = "dataset-name:#{dataset_name}"
relation_name << "#endpoint-file:#{File.basename(endpoint_file)}"
relation_name << "#feature-file:#{File.basename(feature_file)}"
relation_name << "#num-endpoints:#{num_endpoints}"
relation_name << "#num-missing-allowed:#{num_missing_allowed}"
relation_name << "#discretization-level:#{discretization}"
relation_name << "#include-v:#{include_v}"
class_map.each do |k,v|
  relation_name << "#class-value-#{k}:#{v}"
end
outfile = "arff/#{dataset_name}" #"input#{Time.now.strftime("%Y-%m-%d_%H-%M-%S")}"
$stderr.puts "outfile already exists: '#{outfile}'" if File.exist?(outfile+".arff")

endpoint_map = nil
#if(endpoint_file =~ /disc2/ or endpoint_file =~ /discV2/)
#  endpoint_map = {"1" => "0", "2" => "1"}
#elsif(endpoint_file =~ /clust[0-9][0-9]to[0-9][0-9]V/)
#  endpoint_map = {"0" => "0", "1" => "1"}
#elsif(endpoint_file =~ /clust[0-9][0-9]to[0-9][0-9]/)
#  endpoint_map = {"0" => "0", "1" => "1"}
#elsif endpoint_file=~/CPDB/
#  endpoint_map = {"active" => "1", "inactive" => "0", "blank" => "?", "unspecified" => "?"}
#end

additional = ["smiles","name","cas"]
if endpoint_file=~/CPDB/
  additional = ["SMILES","DSSTox_FileID","TestSubstance_ChemicalName","TestSubstance_CASRN"]  
end

arff_file = toArff.to_arff(num_endpoints, num_missing_allowed, relation_name, outfile, endpoint_map, start_endpoint, additional)

puts arff_file

#if (run_eval)
#  cmd = "java -jar mlc.jar -e #{File.basename(endpoint_file)} -f #{File.basename(feature_file)} -n #{num_endpoints} -m #{num_missing_allowed} -x #{num_cores} "+
#    "-o /tmp/result -r #{arff_file} -i #{min_cv_seed} -u #{max_cv_seed_exclusive} -a #{mlc_algorithm}"
#  puts cmd
##  exec cmd
#end
#ProcessUtil.run("")
