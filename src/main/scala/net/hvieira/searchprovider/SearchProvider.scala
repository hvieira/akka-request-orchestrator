package net.hvieira.searchprovider

protected object SearchProvider extends Enumeration {
  type SearchProvider = Value
  val GOOGLE = Value(1)
  val DUCKDUCKGO = Value(2)
  val YAHOO = Value(3)

  def fromInt(param: Int) = {
    SearchProvider.apply(param)
  }

  def url(param: SearchProvider) = param match {
    case GOOGLE => "https://google.com"
    case DUCKDUCKGO => "https://duckduckgo.com"
    case YAHOO => "https://yahoo.com"
  }
}
