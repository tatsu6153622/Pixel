package com.example.docs

import com.example.ui.studio.StudioTab

data class GuideSection(
  val id: String,
  val category: String,
  val title: String,
  val summary: String,
  val steps: List<String>,
  val technicalNote: String,
  val targetTab: StudioTab? = null,
  val actionLabel: String? = null,
)

object DocumentationContent {

  val categories: List<String> = listOf(
    "すべて",
    "動画壁紙拡張",
    "Watch同期",
    "時計・拡張API",
    "省電力最適化",
    "設計・KMP",
  )

  val sections: List<GuideSection> = listOf(
    GuideSection(
      id = "video_extension_bridge",
      category = "動画壁紙拡張",
      title = "既存の「写真のみ壁紙」を動画対応にする拡張機能の仕組み",
      summary = "標準の写真ベース壁紙システムを置き換えずに、2種類の動画拡張アプローチでMP4/MOV壁紙を動作させます。",
      steps = listOf(
        "1. スマホ標準ベース向け【Androidライブ壁紙拡張】: ChronoCanvasはAndroid標準の WallpaperService (BIND_WALLPAPER) を実装しています。『動画壁紙』タブの「システム壁紙拡張として適用」を押すと、OS既存の壁紙ピッカーからそのまま動画壁紙を設定できます。",
        "2. 既存Wear OS / 写真ベースWatch Face向け【フレームシーケンス変換拡張】: 標準のWatch Face Format (WFF) は写真・画像フレームのみ受け付けますが、本アプリ内蔵の VideoExtensionTranscoder が選んだMP4を自動でWatch解像度(456×456 / 15-24fps)・黒マスク付き軽量フレーム列へ最適化し、既存システム互換の形で扱えます。",
        "3. Pixel Watch 3ネイティブ動画再生【:wear ホーム拡張モジュール】: 付属の :wear モジュールをPixel Watch 3に導入すると、Media3 ExoPlayer で滑らかな動画壁紙＋ウィジェット＋アプリドックを直接描画します。",
      ),
      technicalNote = "ストレージ権限（READ_MEDIA_VIDEO等）は一切不要です。Google Play推奨のゼロ権限 Android Photo Picker (PickVisualMedia) で選んだ動画だけをアプリ専用領域へ安全にキャッシュします。",
      targetTab = StudioTab.WALLPAPER,
      actionLabel = "動画壁紙スタジオを開く",
    ),
    GuideSection(
      id = "quickstart_sync",
      category = "Watch同期",
      title = "Watch再インストール不要のOTA自動同期 & リモートアップデート",
      summary = "スマホ連携だけで設定・見た目変更を即時反映し、本体更新が必要な時もスマホからWatch側の更新画面を開けます。",
      steps = listOf(
        "1. 【OTA自動反映（再インストール不要）】『省電力・同期』タブで「変更時にWatchへ自動OTA反映」がONのとき、スマホで時計・X/Y配置・通知発光・壁紙を変更すると約0.7秒後にPixel Watch 3へ自動送信されます。",
        "2. 【Watch再接続時の自動更新】ウォッチの画面をオンにしたり再接続すると PhoneWearSyncService が検知し、最新設定の送信とWatchバージョン照合 (/chronocanvas/watch_status) を自動実行します。",
        "3. 【Watch本体APKの遠隔アップデート】Watchアプリ自体のアップデートが必要な場合も、『省電力・同期』タブの「Watchでストア更新を開く」をタップすると RemoteActivityHelper 経由でPixel Watch 3上にGoogle Play更新画面を直接開けます（Play内部テスト配布URLも送信可能）。",
      ),
      technicalNote = "Android OSのセキュリティおよびGoogle Playポリシー上、Bluetooth経由でサイレントAPK上書きはできませんが、ほとんどのUI・壁紙・機能設定はData LayerのOTA配信で即時反映され、アプリ本体の更新もスマホからのワンタップ遠隔起動で行えます。",
      targetTab = StudioTab.BATTERY_SYNC,
      actionLabel = "同期・アップデートセンターへ",
    ),
    GuideSection(
      id = "launcher_always_on",
      category = "Watch同期",
      title = "電源ON・画面点灯でいつも壁紙ホームを表示する（ランチャー常駐化）",
      summary = "AOD・OngoingActivity・タイル・コンプリケーション・HOMEランチャー登録により、アプリを開き直さず常に表示できます。",
      steps = listOf(
        "1. 【常駐ホーム + AOD（推奨・設定簡単）】Watchで一度ChronoCanvas Watchを開き通知を許可すると、AmbientLifecycleObserver と OngoingActivity により、画面消灯後もシステム文字盤に戻らず電源ボタン/腕上げで即座に動画壁紙へ復帰します。",
        "2. 【1スワイプ・1タップ起動】Pixel Watch 3のタイル追加画面で『ChronoCanvas ホーム』を追加、または既存文字盤のコンプリケーションに『ChronoCanvas 起動』を登録すると、システム文字盤から1操作で戻れます。",
        "3. 【Watchアプリ一覧ドック】右下のアプリボタンからPixel Watch 3にインストール済みの他アプリもそのまま起動できます。",
        "4. 【完全ホームランチャー化（上級）】再起動直後から標準ランチャーの代わりに起動したい場合は、PCから adb shell cmd package set-home-activity com.aistudio.chronocanvas.pw3studio/com.aistudio.chronocanvas.wear.WatchHomeActivity を実行します。",
      ),
      technicalNote = "Wear OS 5は標準設定画面にデフォルトホームアプリ切替UIを持ちませんが、OngoingActivity＋AOD常駐により通常利用ではほぼ常時ホームとして機能します。",
      targetTab = StudioTab.BATTERY_SYNC,
      actionLabel = "ランチャー設定を開く",
    ),
    GuideSection(
      id = "clock_and_extensibility",
      category = "時計・拡張API",
      title = "時計・ウィジェット自由配置・非表示（「なし」）設定",
      summary = "重なりを完全に防ぐX/Y座標スライダー、サイズ・背景透明度、ウィジェットの個別非表示に対応しています。",
      steps = listOf(
        "1. 『時計・拡張』タブでは時計のテキスト色・太さ（細字/標準/太字）・日付形式・不透明度・X/Y位置を細かく調整できます。",
        "2. 各ウィジェットスロット（上・左・右・下）の先頭にある「なし（非表示）」を選ぶか、「すべて非表示」ボタンで動画壁紙や写真だけをすっきり表示できます。",
        "3. 各ウィジェットもX/Y位置・サイズ・背景プレートの濃さ（0%で文字のみ透過）を個別指定できます。「重なり防止配置」で初期位置へ即座に戻せます。",
        "4. 画面下のアプリドックアイコンもトグルで非表示にでき、不要なタグ表示も省かれています。",
      ),
      technicalNote = "デフォルトレイアウトは上・右上・左上・下に分散配置されており、デジタル時計と左右ダイヤルが重ならないよう設計されています。",
      targetTab = StudioTab.CLOCK_WIDGETS,
      actionLabel = "時計・配置エディタへ",
    ),
    GuideSection(
      id = "notifications_and_edge_glow",
      category = "時計・拡張API",
      title = "未読通知件数ウィジェット & フレーム発光アラート",
      summary = "スマホの未読通知件数をウォッチに表示し、着信時にウォッチ外周フレームを任意のカラー・パターンで光らせます。",
      steps = listOf(
        "1. 『時計・拡張』タブ内の「通知カウント連動 & フレーム発光アラート」からOSの「通知へのアクセス」を許可すると、ChronoNotificationListenerService がリアルタイムに未読件数をカウントしWatchへ同期します。",
        "2. 任意のスロットに「未読通知件数」ウィジェットを配置すると件数とアイコンが表示されます。",
        "3. 外周フレーム発光はカラー・太さ・アニメーション（全周パルス発光 / 回転スイープ光 / 点滅アラート）・未読継続点灯をカスタマイズでき、「通知テスト」で即座に発光確認できます。",
      ),
      technicalNote = "AOD（常時表示）省電力中はフレーム発光アニメーションも自動一時停止してバッテリー消費を抑えます。",
      targetTab = StudioTab.CLOCK_WIDGETS,
      actionLabel = "通知・フレーム設定を開く",
    ),
    GuideSection(
      id = "auto_wallpaper_offwrist_time",
      category = "動画壁紙拡張",
      title = "腕時計を外した時 (Off-Wrist) & 時間帯別の自動壁紙切替",
      summary = "Pixel Watch 3の着脱センサーおよび時刻スケジュールに応じて背景壁紙を自動で切り替えます。",
      steps = listOf(
        "1. 『動画壁紙』タブの「腕時計を外した時の壁紙切替」をONにすると、Watch側の物理センサー (TYPE_LOW_LATENCY_OFFBODY_DETECT) が腕から外れたことを検知して、専用動画・画像・省電力純黒画面へ切り替わります。",
        "2. 「時間帯による壁紙自動切替」をONにすると、朝(6-12)・昼(12-18)・夜(18-24)・深夜(0-6) ごとに異なるシェーダーや動画、減光率へ自動移行します。",
        "3. プレビュー下の「装着中 / 外した状態」「実時刻 / 時間帯テスト」チップを使うと、実機を外さずにスマホ上で切り替え結果を確認できます。",
      ),
      technicalNote = "優先順位は「腕時計を外した状態」が最優先、次に「時間帯スケジュール」、通常時はメイン壁紙が適用されます。",
      targetTab = StudioTab.WALLPAPER,
      actionLabel = "自動切替設定を開く",
    ),
    GuideSection(
      id = "battery_optimization",
      category = "省電力最適化",
      title = "Pixel Watch 3 LTPO OLED バッテリー最適化の仕組み",
      summary = "動画壁紙を使いながらバッテリー消費を抑える5層の省電力エンジンを搭載しています。",
      steps = listOf(
        "1. AOD時動画自動停止: 常時表示 (Ambient) に移行した瞬間、Media3 ExoPlayerをpause()し、背景レイヤーを完全にスキップします。",
        "2. LTPO 1Hzアンビエント制御: Pixel Watch 3のLTPOディスプレイ特性に合わせ、AOD時は毎秒1回以下の最小再描画へ自動で落とします。",
        "3. OLED黒マスク減光 (APL制御): 動画の上に半透明の純黒マスクを重ねることで有機ELピクセルの発光電力を物理的に削減し、時計の視認性も高めます。",
        "4. 解像度・FPSスケーリング: ウォッチ画面実寸 (456px / 408px) 以上にデコードしないため、4K動画を選んでもGPU負荷を抑えられます。",
        "5. センサー更新間隔と充電時限定同期: ウィジェット更新周期を30秒〜120秒に調整し、大容量動画転送を充電器接続時のみに制限できます。",
      ),
      technicalNote = "『省電力・同期』タブのパワーアナライザでは、Pixel Watch 3 (45mm: 420mAh / 41mm: 307mAh) に基づく推定連続駆動時間をリアルタイム算出します。",
      targetTab = StudioTab.BATTERY_SYNC,
      actionLabel = "省電力ラボを開く",
    ),
    GuideSection(
      id = "compose_multiplatform_arch",
      category = "設計・KMP",
      title = "Compose Multiplatform 指向モジュラー設計 (:shared / :app / :wear)",
      summary = "高いUI拡張性と再利用性を実現する3モジュール構成の解説です。",
      steps = listOf(
        "1. :shared モジュール: Android固有の Context や MediaPlayer に一切依存しない純粋なCompose UI (SharedWatchHomeCanvas)、スキーマ (WatchHomeConfig)、プラグインレジストリ、省電力エンジンを集約しています。",
        "2. スロット注入パターン: 動画再生サーフェスやプリセット画像は videoWallpaperSlot / presetArtworkSlot ラムダとして外部注入するため、Desktop/iOS等のCompose Multiplatformターゲットにもそのまま流用可能です。",
        "3. :app (スマホコンパニオン) と :wear (Pixel Watch 3ホーム本体) が同一の :shared レンダラーを共有するため、スマホ上のプレビューとウォッチ実機の表示がピクセル単位で一致します。",
      ),
      technicalNote = "設定は WatchConfigCodec により依存ゼロのJSONスキーマへシリアライズされ、Roomデータベース・DataStore・Wear OS Data Layer間で共通利用されます。",
      targetTab = StudioTab.STUDIO,
      actionLabel = "スタジオプレビューを見る",
    ),
    GuideSection(
      id = "troubleshooting_faq",
      category = "Watch同期",
      title = "トラブルシューティング & よくある質問",
      summary = "動画が選ばれない・同期ボタンでノードが見つからない場合の確認ポイント。",
      steps = listOf(
        "Q. 動画を選んでも時計の文字が読みにくい → 『動画壁紙』タブで「OLED黒マスク減光」を35%〜55%に上げると、節電と視認性を両立できます。",
        "Q. 『ノード未検出』と表示される → スマホとPixel Watch 3のペアリング、Bluetooth ON、および両端末への同一署名APKインストールを確認してください。",
        "Q. システムのライブ壁紙を設定したのに動画が動かない → 別のアプリ等で動画ファイルURIが消えた場合に備え、ChronoCanvasは選択時にアプリ内キャッシュ (active_video.mp4) へコピーします。再選択して『システム壁紙拡張として適用』を押してください。",
      ),
      technicalNote = "ライブ壁紙サービスは画面非表示時 (onVisibilityChanged = false) に自動でプレーヤーを一時停止するため、ホーム画面を見ていない間の電池消費はゼロです。",
      targetTab = StudioTab.WALLPAPER,
      actionLabel = "壁紙設定を確認する",
    ),
  )
}
