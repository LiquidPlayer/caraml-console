Pod::Spec.new do |s|
  s.name         = "caraml-console"
  s.version      = "0.1.0"
  s.summary      = "ANSI console addon for caraml"

  s.description  = <<-DESC
    An ANSI console view for use with caraml and LiquidCore.
  DESC

  s.homepage     = "https://github.com/LiquidPlayer/caraml-console"
  s.license      = {:type => "MIT", :file => "LICENSE.md"}

  s.author       = { "Eric Lange" => "eric@flicket.tv" }

  s.platform     = :ios, '10.0'

  s.source = { :git => "https://github.com/LiquidPlayer/caraml-console.git", :tag => "#{s.version}" }

  s.source_files  =
    "src/*.{cpp,h}",
    "caraml-console/caraml-console/*.{h,m,mm}"

  s.public_header_files =
    "caraml-console/caraml-console/caraml_console.h"

  s.xcconfig = {
    :CLANG_WARN_DOCUMENTATION_COMMENTS => 'NO'
  }

  s.resources = [
    'caraml-console/caraml-console/ConsoleView.xib',
  ]

  s.swift_version = '3.0'

  s.dependency "LiquidCore"
  s.dependency "caraml-core"
end
