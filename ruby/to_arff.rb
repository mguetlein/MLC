
require "rubygems"
require "csv"


class ToArff
  
  def initialize(endpoint_file, feature_file)
    
    @files = [feature_file, endpoint_file]
    key_pattern = /(CAS|ID)/
    
    @store = {}
    @id_order = []
    @key_order = []
    @key_file_index = []
    @num_occ = {}
    @endpoints = []
    
    @files.each_with_index do |file,file_count|

      $stderr.puts file
      s = File.open(file, 'rb') { |f| f.read }
      sep = s.count(",")>s.count(";") ? "," : ";"
      
      id_index = nil
      ids = []
      keys = []
      
      CSV.parse(File.new(file),{:col_sep=>sep}).each_with_index do |row,row_index|
        #puts row_index.to_s+" "+row.join(",")
        raise if row.length<2
        if row_index==0
          keys = row
          keys.each do |k|
            raise "duplicate key: #{k} in #{file}" if keys.count(k)>1
          end
          keys.each_with_index do |k,i|
            if k=~key_pattern
              id_index = i
              break
            end
          end
          raise "no id match #{row}" if id_index==nil
        else
          id = row[id_index]
          ids << id
          row.each_with_index do |r,i|
            if i!=id_index
              @store[ [ id,keys[i] ] ] = [] unless @store.has_key?([ id,keys[i] ])
              @store[ [ id,keys[i] ] ] << r unless i==id_index and @store[ [ id,keys[i] ] ].size>0 
              @num_occ[ id ] = [ (@num_occ[ id ] ? @num_occ[ id ] : 0) , @store[ [ id,keys[i] ] ].size ].max
            end
          end
        end    
      end
      
      #puts keys.inspect
    
      sel_keys = []  
      keys.each do |k|
        sel_keys << k unless ["CAS","ID","SMILES","Name","study_pk","route","route2","study_duration","Jahr","reliability", "dummy"].include?(k)
      end
    
      @endpoints = sel_keys - [keys[id_index]] if file==endpoint_file
      
      @key_order += sel_keys
      @key_file_index += Array.new(keys.size,file_count)
      $stderr.puts "duplicate keys exist" if @key_order.uniq.size!=@key_order.size
      @key_order.uniq!
      @id_order += ids
      @id_order.uniq!
      
    end
      
    $stderr.puts "num compounds #{@id_order.size}"
    @sum = 0
    @num_occ.each do |k,v|
      @sum += v
    end
    
    $stderr.puts "num instances (compounds plus duplicates) #{@sum}"
    $stderr.puts "num features #{@key_order.size-(@endpoints.size+1)}"

    puts @endpoints.size.to_s+" endpoints"
    #puts @endpoints.inspect    
    sort_endpoints
    @key_order = (@key_order - @endpoints) + @endpoints 
    #$stderr.puts "fields: #{@key_order.inspect}"
    #$stderr.puts ""
  end

  def sort_endpoints()      
    @endpoint_counts = {}
    @endpoints.each do |e|
      nil_count = 0
      @id_order.each do |id|
        next unless @store[ [id,e] ] # not stored in endpoint file
        @num_occ[id].times do |i|
          if @store[ [id,e] ].size==1
            nil_count+=1 if @store[ [id,e] ][0]==nil
          else
            nil_count+=1 if @store[ [id,e] ][i]==nil
          end
        end
      end
      @endpoint_counts[e] = nil_count
    end
    #$stderr.puts @endpoint_counts.inspect
    @endpoints = @endpoints.sort{|a,b| (@endpoint_counts[a]==@endpoint_counts[b] ? @endpoints.index(a)<=>@endpoints.index(b) : @endpoint_counts[a]<=>@endpoint_counts[b])}
  end
  
  def num_max_endpoints
    @endpoints.size
  end
  
  def to_arff(num_endpoints, num_missing_allowed, relation_name, outfile, endpoint_value_map=nil)
  
    raise if num_endpoints>@endpoints.size
    raise if num_missing_allowed>num_endpoints
    endpoints = @endpoints[0..(num_endpoints-1)]
  
    file_selected_key_pattern = [/[^ID]/,/^#{endpoints.join("$|^")}$/]
    sel_key_order = []
    @key_order.each_with_index do |k,i|
      if k=~file_selected_key_pattern[@key_file_index[i]]
        sel_key_order << k
      end
    end
  
    #title = Time.now.strftime("%Y-%m-%d_%H-%M-%S")+"__#{num_endpoints}endpoints_#{num_missing_allowed}missingAllowed"
    
    not_numeric = {}
    endpoints.each do |e|
      not_numeric[e] = ["0","1"]
    end
    
    $stderr.puts "selected endpoints: #{endpoints.inspect}"
    #$stderr.puts "num selected fields #{sel_key_order.size}/#{@key_order.size}"
    #$stderr.puts "fields: #{sel_key_order.inspect}"
    
    $stderr.puts "print to "+outfile+".arff"
    f = File.open(outfile+".arff","w+") 
    
    f.puts "% merged from:"
    @files.each{|file| f.puts "% #{file}"}
    f.puts "@RELATION #{relation_name}"
    f.puts ""
    
    sel_key_order.each do |k|
      if not_numeric.has_key?(k)
        f.puts "@ATTRIBUTE \"#{k.gsub(/\./,"_")}\" {#{not_numeric[k].join(",")}}"
      else
        f.puts "@ATTRIBUTE \"#{k.gsub(/\./,"_")}\" NUMERIC"
      end
    end
    f.puts ""
    
    num_data = 0
    
    f.puts "@DATA"
    @id_order.each do |id|
      next unless @store[ [id,endpoints[0]] ] # not stored in endpoint file
      @num_occ[id].times do |i|
        nil_count = 0
        endpoints.each do |e|
          if @store[ [id,e] ].size==1
            nil_count+=1 if @store[ [id,e] ][0]==nil
          else
            nil_count+=1 if @store[ [id,e] ][i]==nil
          end
        end
        if num_missing_allowed>=nil_count
          num_data += 1
          s = ""
          sel_key_order.each do |k|
            raise "#{id} missing: #{k}" unless @store[ [id,k] ]
            raise "#{id} num-occurences: #{@num_occ[id]} values for #{k} : #{@store[ [id,k] ].inspect}" unless @store[ [id,k] ].size==1 || @store[ [id,k] ].size==@num_occ[id]
            if @store[ [id,k] ].size==1
              v = (@store[ [id,k] ][0] ? @store[ [id,k] ][0] : "?") 
            else
              v = (@store[ [id,k] ][i] ? @store[ [id,k] ][i] : "?") 
            end
            if endpoints.include?(k)
              if endpoint_value_map and v!="?" 
                raise "WTF: '#{v}'" unless endpoint_value_map.has_key?(v)
                v = endpoint_value_map[v]
              else
                raise "WTF: '#{v}'" unless ["0","1","?"].include?(v)
              end 
            end
            s << v
            s << ","
          end
          f.puts s.chop
        end
      end
    end 
    f.close
    $stderr.puts "printed #{num_data}/#{@sum} instances"
    
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