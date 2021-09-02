module Config
  module Helpers
    def without_timestamps(hash)
      hash.without("created_at", "updated_at")
    end
  end
end
