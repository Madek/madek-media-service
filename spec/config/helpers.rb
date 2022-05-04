module Config
  module Helpers

    def without_timestamps(hash)
      hash.without("created_at", "updated_at")
    end

    def wait_until(wait_time = 5, &block)
      Timeout.timeout(wait_time) do
        until value = yield
          sleep(0.5)
        end
        value
      end
    rescue Timeout::Error => e
      raise Timeout::Error.new(block.source)
    end


  end
end
