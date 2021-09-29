module Config
  module HTTPClient
    def plain_faraday_client
      Faraday.new(
        url: "#{base_url}/media-service/",
        headers: {
          accept: "application/json",
          cookie: [session_cookie, anti_csrf_token_cookie].compact.join("; ")
        }
      ) do |f|
        f.request :json
        f.response :json
        f.headers["Madek-Media-Service-Anti-Csrf-Token"] = anti_csrf_token_value if anti_csrf_token_value
      end
    end

    def faraday_client_with_token(json_response: false)
      Faraday.new(
        url: "#{base_url}/media-service/",
        headers: {
          accept: "application/json"
        }
      ) do |f|
        f.request :json
        f.response :json if json_response
        f.headers["Authorization"] = "Token #{user_token}" if user_token
      end
    end

    def faraday_client_for_upload
      Faraday.new(
        url: "#{base_url}/media-service/uploads/#{upload_id}/",
        headers: {
          accept: "application/json"
        }
      ) do |f|
        f.request :json
        f.response :json
        f.headers["Content-Type"] = "application/octet-stream"
        f.headers["Authorization"] = "Token #{user_token}" if user_token
      end
    end

    def get_anti_csrf_token
      Faraday.get(
        "#{base_url}/media-service/",
        nil,
        { cookie: session_cookie }
      ).headers["set-cookie"]
    end

    private

    def session_cookie
      "madek-session=#{MadekOpenSession.build_session_value(user)}" if user
    end

    def anti_csrf_token_value
      anti_csrf_token
        .remove("madek-media-service-anti-csrf-token=")
        .remove(";Path=/") if try(:anti_csrf_token).present?
    end

    def anti_csrf_token_cookie
      "madek-media-service-anti-csrf-token=#{anti_csrf_token_value}" if anti_csrf_token_value
    end
  end
end
