
require "rubygems"
require "csv"


class ToArff
  
  def initialize(endpoint_file, feature_file)
    
    @files = [endpoint_file, feature_file]
    key_pattern = /^(CAS|ID)$/
    
#    @store = {}
#    @id_order = []
#    @key_order = []
#    @key_file_index = []
#    @num_occ = {}
#    @endpoints = []

    @head = []
    @data = []
    @id = []
    @features = nil
    @endpoints = nil
    
    @files.each do |file|

      id_index = nil
      $stderr.puts file
      s = File.open(file, 'rb') { |f| f.read }
      sep = s.count(",")>s.count(";") ? "," : ";"
      CSV.parse(File.new(file),{:col_sep=>sep}).each_with_index do |row,row_index|
        raise if row.length<2
        if row_index==0
          row.each{|r| raise "duplicate key: #{r} in #{file}" if row.count(r)>1}
          id_index=nil
          row.each_with_index{|k,i| id_index=i if k=~key_pattern}      
          if file==endpoint_file
            @head = row
            @endpoints = row
          else
            @head += row
            @features = row
          end
        else
          if file==endpoint_file
            @data << row
            @id << row[id_index]
          else
            feature_row_id = row[id_index]
            @data.size.times do |i|
              @data[i] += row if @id[i]==feature_row_id
            end
          end
        end
      end
    end    
    
    stuff = ["CAS","ID","SMILES","Name","study_pk","route","route2","study_duration","Jahr","reliability", "dummy"]
    @endpoints -= stuff
    @features -= stuff
      
    $stderr.puts "num compounds #{@id.size} (non-duplicates: #{@id.uniq.size})"
    $stderr.puts "num features #{@features.size}"
    $stderr.puts "num endpoints #{@endpoints.size}"
    sort_endpoints
  end

  def sort_endpoints()      
    @endpoint_counts = {}
    @endpoints.each do |e|
      index = @head.index(e)
      nil_count = 0
      @data.each do |vals|
        nil_count+=1 if vals[index]==nil
      end
      @endpoint_counts[e] = nil_count
    end
    @endpoints = @endpoints.sort{|a,b| (@endpoint_counts[a]==@endpoint_counts[b] ? @endpoints.index(a)<=>@endpoints.index(b) : @endpoint_counts[a]<=>@endpoint_counts[b])}
  end
  
  def num_max_endpoints
    @endpoints.size
  end
  
  def to_arff(num_endpoints, num_missing_allowed, relation_name, outfile, endpoint_value_map=nil)
  
    raise if num_endpoints>@endpoints.size
    raise if num_missing_allowed>num_endpoints
    endpoints = @endpoints[0..(num_endpoints-1)]
  
    not_numeric = {}
    endpoints.each do |e|
      not_numeric[e] = ["0","1"]
    end
    $stderr.puts "selected endpoints: #{endpoints.inspect}"
    
    $stderr.puts "print to "+outfile+".arff"
    f = File.open(outfile+".arff","w+")
    $stderr.puts "print to "+outfile+".csv"
    f2 = File.open(outfile+".csv","w+")  
    
    f.puts "% merged from:"
    @files.each{|file| f.puts "% #{file}"}
    f.puts "@RELATION #{relation_name}"
    f.puts ""
    
    (@features+endpoints).each do |k|
      if not_numeric.has_key?(k)
        f.puts "@ATTRIBUTE \"#{k.gsub(/\./,"_")}\" {#{not_numeric[k].join(",")}}"
      else
        f.puts "@ATTRIBUTE \"#{k.gsub(/\./,"_")}\" NUMERIC"
      end
    end
    f.puts ""
    f2.puts '"'+(["SMILES","Name","CAS"]+@features+endpoints).join('","')+'"'
    
    num_data = 0
    
    f.puts "@DATA"
    @data.each do |vals|
      nil_count = 0
      endpoints.each do |e|
        index = @head.index(e)
        nil_count+=1 if vals[index]==nil
      end
      if num_missing_allowed>=nil_count
        num_data += 1
        values = []
        (["SMILES","Name","CAS"]+@features+endpoints).each do |k|
          index = @head.index(k)
          v = vals[index]
          v = "?" if v==nil
          if endpoints.include?(k)
            if endpoint_value_map and v!="?" 
              raise "WTF: '#{v}'" unless endpoint_value_map.has_key?(v)
              v = endpoint_value_map[v]
            else
              raise "WTF: '#{v}'" unless ["0","1","?"].include?(v)
            end 
          end
          values << v
        end
        raise "WTF #{values[3..-1].size} != #{(@features+endpoints).size}" unless values[3..-1].size==(@features+endpoints).size
        f.puts values[3..-1].join(",")
        f2.puts(('"'+values.collect{|v| v=="?" ? "" : v}.join('","')+'"').gsub(',""',','))
      end
    end 
    f.close
    f2.close
    $stderr.puts "printed #{num_data}/#{@data.size} instances"
    
    $stderr.puts "print to "+outfile+".xml"
    f = File.open(outfile+".xml","w+")
    f.puts '<?xml version="1.0" encoding="utf-8"?>'
    f.puts '<labels xmlns="http://mulan.sourceforge.net/labels">'
    endpoints.each do |endpoint|   
      f.puts '<label name="'+endpoint.gsub(/\./,"_")+'"></label>'
    end
    f.puts '</labels>'
    f.close
    
    
    $stderr.puts ""
    #outfile+".arff"
  end      
  
end