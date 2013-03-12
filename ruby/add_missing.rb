

file = ARGV[0]
sep = ";"

contents = File.read(file).split("\n")
puts contents.delete_at(0)

missing_ratios = nil

contents.each do |line|
  vals = line.split(sep)
  unless missing_ratios
     missing_ratios = []
     (vals.size-1).times do 
       missing_ratios << rand
     end
  end
  (vals.size-1).times do |i|
    vals[i+1] = nil if rand < missing_ratios[i]
  end
  puts vals.join(";")
end

$stderr.puts "missing props were:\n"+missing_ratios.inspect

