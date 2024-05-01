require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-brightcove-ima-player"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "11.0" }
  s.source       = { :git => "https://github.com/NZME/react-native-brightcove-ima-player.git", :tag => "v#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm}"

  s.dependency "React-Core"
  s.dependency 'Google-Mobile-Ads-SDK', '11.2.0'
  s.dependency "Brightcove-Player-IMA/XCFramework"
  s.dependency 'Brightcove-Player-SSAI/XCFramework'
end
