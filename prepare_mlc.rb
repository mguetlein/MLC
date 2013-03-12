require "rubygems"
require "getoptlong"

require "./ruby/to_arff.rb"

#require 'open4'
#module ProcessUtil
#  def self.run(cmd)
#    puts cmd
#    status = Open4::popen4(cmd) do |pid, stdin, stdout, stderr|
#      puts "pid        : #{ pid }"
#      puts "stdout     : #{ stdout.read.strip }"
#      puts "stderr     : #{ stderr.read.strip }"
#    end    
#    status    
#  end
#end


opts = GetoptLong.new(
  [ '--endpoint-file', '-e', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--feature-file', '-f', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--num-endpoints', '-n', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--num-missing-allowed', '-m', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--num-cores', '-x', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--min-cv-seed', '-i', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--max-cv-seed-exclusive', '-u', GetoptLong::REQUIRED_ARGUMENT ],
  [ '--mlc-algorithm', '-a', GetoptLong::REQUIRED_ARGUMENT ],
)

usage = "Usage: ..."

endpoint_file = nil
feature_file = nil
num_endpoints = nil
num_missing_allowed = nil

num_cores = nil
min_cv_seed = 0
max_cv_seed_exclusive = 3
mlc_algorithm = "ECC"

run_eval = false

opts.each do |opt, arg|
  case opt
  when '--endpoint-file'
    endpoint_file = arg
  when '--feature-file'
    feature_file = arg
  when '--num-endpoints'
    num_endpoints = arg
  when '--num-missing-allowed'
    num_missing_allowed = arg
  when '--num-cores'
    num_cores = arg
    run_eval = true
  when '--min-cv-seed'
    min_cv_seed = arg.to_i
    run_eval = true
  when '--max-cv-seed-exclusive'
    max_cv_seed_exclusive = arg
    run_eval = true
  when '--mlc-algorithm'
    mlc_algorithm = arg
    run_eval = true
  end  
end

raise "enpoint-file missing\n"+usage unless endpoint_file
raise unless File.exist? endpoint_file
puts "Endpoint file: "+endpoint_file

raise "feature-file missing\n"+usage unless feature_file
raise unless File.exist? feature_file
puts "Feature file: "+feature_file

raise "num-endpoints\n"+usage unless num_endpoints
puts "Num endpoints: #{num_endpoints==-1 ? "all" : num_endpoints}"

raise "num-missing-allowed\n"+usage unless num_missing_allowed
puts "Num missing allowed: #{num_missing_allowed==-1 ? "all" : num_missing_allowed}"

toArff = ToArff.new(endpoint_file, feature_file)
num_endpoints = num_endpoints=="all" ? toArff.num_max_endpoints : num_endpoints.to_i
num_missing_allowed = num_missing_allowed=="all" ? num_endpoints : num_missing_allowed.to_i
map = nil
if(endpoint_file =~ /disc2/)
  map = {"1" => "0", "2" => "1"}
end
arff_file = toArff.to_arff(num_endpoints, num_missing_allowed, "arff/",map)

if (run_eval)
  cmd = "java -jar mlc.jar -e #{File.basename(endpoint_file)} -f #{File.basename(feature_file)} -n #{num_endpoints} -m #{num_missing_allowed} -x #{num_cores} "+
    "-o /tmp/result -r #{arff_file} -i #{min_cv_seed} -u #{max_cv_seed_exclusive} -a #{mlc_algorithm}"
  puts cmd
#  exec cmd
end
#ProcessUtil.run("")
