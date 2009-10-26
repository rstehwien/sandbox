#!/usr/bin/ruby
%w[pp rubygems xml].each { |x| require x }

$link_report_classes = {}

def process_script(script)
  dat = {
    :name => script.find_first("def")["id"],
    :file => script["name"],
    :inherits => script.find("pre").map {|x| x["id"]},
    :associations => script.find("dep").map {|x| x["id"]}
  }
end

def process_file(f)
  parser = XML::Parser.file(f)
  doc = parser.parse
  doc.find("//report/scripts/script").each {|script|
    dat = process_script script
    name = dat[:name]

    if $link_report_classes.key?(name)
      $link_report_classes[name][:inherits] = $link_report_classes[name][:inherits] | dat[:inherits]
      $link_report_classes[name][:associations] = $link_report_classes[name][:associations] | dat[:associations]
    else
      $link_report_classes[name] = dat 
    end
  }
end

link_report_files = Dir['src/LatestApi/**/*linkreport.xml']

link_report_files.each {|f|
  process_file f
  }
#process_file "./linkreport.xml"

#pp $link_report_classes

def fix_node_name(name)
#  name.gsub(/[^a-zA-Z0-9]/, "_")
  "\"#{name}\""
end

def write_nodes(f, node_hash)
  $link_report_classes.each {|key, value|
    f.puts("#{fix_node_name(value[:name])} [")
    f.puts("\tLabel=\"#{value[:name]}\"")
    url = value[:name].gsub(/[.:]/, "/") + ".html"
    f.puts("\tURL=\"#{url}\"")
    f.puts("]")
  }
end

def write_links(f, node_hash, link_key, style)
  node_hash.each {|key, value|
    value[link_key].each {|i|
      f.puts("#{fix_node_name(value[:name])} -> #{fix_node_name(i)}#{style};")
    }
  }
end

File.open("linkreport.dot", "w") {|f|
  f.puts("digraph graphname {")
  f.puts("graph [
  rankdir = \"RL\"
  ];")
  
  write_nodes(f, $link_report_classes)
  write_links(f, $link_report_classes, :inherits, "")
  #write_links(f, $link_report_classes, :associations, "[style=\"dashed\"]")
  
  f.puts("}")
}