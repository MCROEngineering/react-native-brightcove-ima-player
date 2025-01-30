//
//  BrightcoveIMAPlayerNowPlayingHandler.swift
//  Pods
//
//  Created by Mihai Chifor on 28.01.2025.
//

import UIKit
import BrightcovePlayerSDK

@objcMembers
public final class BrightcoveIMAPlayerNowPlayingHandler: NSObject {
  
  fileprivate weak var session: BCOVPlaybackSession?
  
  fileprivate lazy var nowPlayingInfo: [String: AnyHashable] = [:]
  
  @objc
  public init(with playbackController: BCOVPlaybackController) {
    super.init()
    
    playbackController.add(self)
    
    let center = MPRemoteCommandCenter.shared()
    center.pauseCommand.addTarget { _ in
      playbackController.pause()
      return .success
    }
    
    center.playCommand.addTarget { _ in
      playbackController.play()
      return .success
    }
    
    center.changePlaybackPositionCommand.addTarget { command in
      guard let playbackPositionCommandEvent = command as? MPChangePlaybackPositionCommandEvent else {
        return .commandFailed
      }
      
      let seconds = CMTime(seconds: playbackPositionCommandEvent.positionTime, preferredTimescale: 600)
      playbackController.seek(to: seconds, completionHandler: nil)
      
      return .success
    }
    
    center.togglePlayPauseCommand.addTarget { [self] _ in
      guard let session else { return .commandFailed }
      
      if session.player.timeControlStatus == .paused {
        playbackController.play()
      } else {
        playbackController.pause()
      }
      
      return .success
    }
  }
  
  @objc
  public func updateNowPlayingInfoForAudioOnly() {
    guard let customFields = session?.video.properties["custom_fields"] as? [String: Any] else {
      return
    }
    
    nowPlayingInfo[MPMediaItemPropertyMediaType] = MPMediaType.music.rawValue
    
    // These custom_fields values can be configured in VideoCloud
    // https://beacon.support.brightcove.com/syncing-with-video-cloud/vc-custom-fields.html
    
    if let albumName = customFields["album_name"] as? String {
      nowPlayingInfo[MPMediaItemPropertyAlbumTitle] = albumName
    }
    
    if let albumArtist = customFields["album_artist"] as? String {
      nowPlayingInfo[MPMediaItemPropertyArtist] = albumArtist
    }
    
    let infoCenter = MPNowPlayingInfoCenter.default()
    infoCenter.nowPlayingInfo = nowPlayingInfo
  }
}


extension BrightcoveIMAPlayerNowPlayingHandler {
  
  public override func observeValue(forKeyPath keyPath: String?,
                                    of object: Any?,
                                    change: [NSKeyValueChangeKey: Any]?,
                                    context: UnsafeMutableRawPointer?) {
    
    if let session = self.session as? NSObject,
       let object = object as? NSObject,
       session == object,
       keyPath == "player.rate",
       let change,
       let rate = change[.newKey] as? NSNumber {
      let infoCenter = MPNowPlayingInfoCenter.default()
      nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = rate
      infoCenter.nowPlayingInfo = nowPlayingInfo
      self.nowPlayingInfo = nowPlayingInfo
    }
  }
}


// MARK: - BCOVPlaybackSessionConsumer

extension BrightcoveIMAPlayerNowPlayingHandler: BCOVPlaybackSessionConsumer {
  
  public func didAdvance(to session: BCOVPlaybackSession!) {
    
    if let prevSession = self.session as? NSObject {
      prevSession.removeObserver(self,
                                 forKeyPath: "player.rate")
    }
    
    self.session = session
    
    if let newSession = session as? NSObject {
      newSession.addObserver(self,
                             forKeyPath: "player.rate",
                             options: [.new, .initial],
                             context: nil)
    }
    
    nowPlayingInfo = [String: AnyHashable]()
    
    guard let videoName = session.video.properties[kBCOVVideoPropertyKeyName],
          let durationNum = session.video.properties[kBCOVVideoPropertyKeyDuration] as? NSNumber else {
      return
    }
    
    let duration = Double(durationNum.doubleValue / 1000)
    
    nowPlayingInfo[MPMediaItemPropertyTitle] = videoName as? AnyHashable
    nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = NSNumber(floatLiteral: duration)
    nowPlayingInfo[MPMediaItemPropertyMediaType] = MPMediaType.anyVideo.rawValue
    
    let infoCenter = MPNowPlayingInfoCenter.default()
    infoCenter.nowPlayingInfo = nowPlayingInfo
    
    if let posterURL = session.video.properties[kBCOVVideoPropertyKeyPoster] as? String,
       let url = URL(string: posterURL) {
      DispatchQueue.global(qos: .background).async { [self] in
        do {
          let imageData = try Data(contentsOf: url)
          if let image = UIImage(data: imageData) {
            nowPlayingInfo[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(boundsSize: image.size) {
              _ -> UIImage in
              return image
            }
            
            let infoCenter = MPNowPlayingInfoCenter.default()
            infoCenter.nowPlayingInfo = nowPlayingInfo
          }
        } catch {
          print("Error getting thumbnail image data: \(error.localizedDescription)")
        }
      }
    }
  }
  
  public func playbackSession(_ session: BCOVPlaybackSession!,
                              didProgressTo progress: TimeInterval) {
    if progress.isInfinite { return }
    let infoCenter = MPNowPlayingInfoCenter.default()
    nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = NSNumber(integerLiteral: Int(progress))
    infoCenter.nowPlayingInfo = nowPlayingInfo
  }
}
