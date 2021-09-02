require 'requests/shared/authorization_error'

describe "Uploads", type: :request do
  let(:file) { File.read("spec/support/files/small.txt") }
  let(:file_size) { 132 }
  let(:part_1) { File.read("spec/support/files/small_01") }
  let(:part_2) { File.read("spec/support/files/small_02") }
  let(:md5) { Digest::MD5.hexdigest(file) }
  let(:part_1_md5) { Digest::MD5.hexdigest(part_1) }
  let(:part_2_md5) { Digest::MD5.hexdigest(part_2) }
  let(:start_request) { faraday_client_with_token.post("settings/uploads/#{upload_id}/start") }
  let(:complete_request) do
    faraday_client_with_token.post("settings/uploads/#{upload_id}/complete")
  end
  let(:upload_id) do
    faraday_client_with_token(json_response: true)
      .post(
        "settings/uploads/",
        content_type: "text/plain",
        filename: "small.txt",
        md5: md5,
        media_store_id: store.id,
        size: file_size
      )
      .body
      .fetch("id")
  end
  let(:api_token) { create(:api_token, user: user, scope_write: true) }
  let(:user_token) { api_token.token_hash }

  describe "GET: settings/uploads/" do
    let(:response) { faraday_client_with_token.get("settings/uploads/") }

    context "with public access" do
      let(:user) { nil }

      it_raises "authorization error"
    end

    context "for an ordinary user" do
      let(:user) { create(:user) }

      it_raises "authorization error"
    end

    context "for an user with admin role" do
      let(:user) { create(:user, :with_admin_role) }

      it_raises "authorization error"
    end

    context "for user with system admin role" do
      let(:user) { create(:user, :with_system_admin_role) }

      it "responds with success" do
        expect(response.status).to eq(200)
      end
    end
  end

  describe "POST: settings/uploads/" do
    let!(:store) { create(:media_store, :database, :with_users, users: [user].compact) }
    let(:request) do
      faraday_client_with_token.post("settings/uploads/",
                                content_type: "text/plain",
                                filename: "small.txt",
                                md5: md5,
                                media_store_id: store.id,
                                size: file_size)
    end
    let(:response) { request }
    let(:parsed_body) { JSON.parse(response.body) }

    context "with public access" do
      let(:user) { nil }

      it_raises "authorization error"
    end

    context "for an ordinary user" do
      let(:user) { create(:user) }

      it_raises "authorization error"
    end

    context "for an user with admin role" do
      let(:user) { create(:user, :with_admin_role) }

      it_raises "authorization error"
    end

    context "for user with system admin role" do
      let(:user) { create(:user, :with_system_admin_role) }

      it "responds with success" do
        expect(response.status).to eq(200)
      end

      it "returns upload details" do
        expect(without_timestamps(parsed_body))
          .to match({
            id: a_kind_of(String),
            md5: md5,
            state: "announced",
            media_store_id: "database",
            size: file_size,
            uploader_id: user.id,
            content_type: "text/plain",
            filename: "small.txt",
            media_file_id: nil,
            error: nil,
            sha256: nil,
          }.stringify_keys)
      end
    end
  end

  describe "POST: settings/uploads/:upload_id/start" do
    let(:response) { start_request }
    let(:store) { create(:media_store, :database, :with_users, users: [user].compact) }

    context "with public access" do
      let(:user) { nil }
      let(:upload_id) { :not_relevant }

      it_raises "authorization error"
    end

    context "for an ordinary user" do
      let(:user) { create(:user) }
      let(:upload_id) { :not_relevant }

      it_raises "authorization error"
    end

    context "for an user with admin role" do
      let(:user) { create(:user, :with_admin_role) }
      let(:upload_id) { :not_relevant }

      it_raises "authorization error"
    end

    context "for user with system admin role" do
      let(:user) { create(:user, :with_system_admin_role) }
      let(:parsed_body) { JSON.parse(response.body) }

      it "responds with success" do
        expect(response.status).to eq(200)
      end

      it "returns upload details with 'started' state" do
        expect(without_timestamps(parsed_body)).to eq(
          {
            id: upload_id,
            md5: md5,
            state: "started",
            media_store_id: "database",
            size: file_size,
            uploader_id: user.id,
            content_type: "text/plain",
            filename: "small.txt",
            media_file_id: nil,
            error: nil,
            sha256: nil
          }.stringify_keys
        )
      end
    end
  end

  describe "POST: settings/uploads/:upload_id/complete" do
    let(:response) { complete_request }
    let(:store) { create(:media_store, :database, :with_users, users: [user].compact) }

    context "with public access" do
      let(:user) { nil }
      let(:upload_id) { :not_relevant }

      it_raises "authorization error"
    end

    context "for an ordinary user" do
      let(:user) { create(:user) }
      let(:upload_id) { :not_relevant }

      it_raises "authorization error"
    end

    context "for an user with admin role" do
      let(:user) { create(:user, :with_admin_role) }
      let(:upload_id) { :not_relevant }

      it_raises "authorization error"
    end

    context "for user with system admin role" do
      let(:user) { create(:user, :with_system_admin_role) }
      let(:parsed_body) { JSON.parse(response.body) }

      context "when upload has 'started' state" do
        before do
          faraday_client_with_token.post("settings/uploads/#{upload_id}/start")
        end

        it "responds with success" do
          expect(response.status).to eq(200)
        end

        it "returns upload details with 'completed' state" do
          expect(without_timestamps(parsed_body)).to eq(
            {
              id: upload_id,
              md5: md5,
              state: "completed",
              media_store_id: "database",
              size: file_size,
              uploader_id: user.id,
              content_type: "text/plain",
              filename: "small.txt",
              media_file_id: nil,
              error: nil,
              sha256: nil
            }.stringify_keys
          )
        end
      end

      context "when upload has 'announced' state" do
        let(:response) { complete_request }

        it "responds with 500" do
          expect(response.status).to eq(500)
        end

        it "responds with error message" do
          expect(response.body).to eq("expected upload state to be started but is announced")
        end
      end
    end
  end

  describe "sending file parts" do
    let!(:settings) { create(:media_service_setting, upload_min_part_size: chunk_size) }
    let(:store) { create(:media_store, :database, :with_users, users: [user]) }
    let(:user) { create(:user, :with_system_admin_role) }
    let(:chunk_size) { 100 }
    def upload_get_request
      faraday_client_with_token(json_response: true).get("settings/uploads/#{upload_id}")
    end

    before do
      start_request
    end

    let(:part_1_request) do
      faraday_client_for_upload.put("parts/0", part_1) do |req|
        req.params = {
          start: 0,
          size: chunk_size,
          md5: part_1_md5
        }
      end
    end

    let(:part_2_request) do
      faraday_client_for_upload.put("parts/1", part_2) do |req|
        req.params = {
          start: chunk_size,
          size: 32,
          md5: part_2_md5
        }
      end
    end

    it "uploads file in 2 parts" do
      expect(without_timestamps(part_1_request.body)).to match({
        id: a_kind_of(String),
        md5: part_1_md5,
        media_file_id: nil,
        part: 0,
        sha256: Digest::SHA256.hexdigest(part_1),
        size: chunk_size,
        start: 0,
        upload_id: upload_id,
      }.stringify_keys)

      expect(without_timestamps(part_2_request.body)).to match({
        id: a_kind_of(String),
        md5: part_2_md5,
        media_file_id: nil,
        part: 1,
        sha256: Digest::SHA256.hexdigest(part_2),
        size: 32,
        start: chunk_size,
        upload_id: upload_id,
      }.stringify_keys)
    end

    describe "GET: settings/uploads/:upload_id" do
      context "when all parts of file have been sent" do
        before do
          part_1_request          
          part_2_request
          complete_request
        end

        it "responds with upload details with 'completed' state" do
          expect(without_timestamps(upload_get_request.body)).to eq({
            id: upload_id,
            md5: md5,
            state: "completed",
            media_store_id: "database",
            size: file_size,
            uploader_id: user.id,
            content_type: "text/plain",
            filename: "small.txt",
            media_file_id: nil,
            error: nil,
            sha256: nil,
          }.stringify_keys)
        end

        it "2nd call responds with upload details with 'finished' state" do
          upload_get_request
          sleep 1 # ugly!
          expect(without_timestamps(upload_get_request.body)).to match({
            id: upload_id,
            md5: md5,
            state: "finished",
            media_store_id: "database",
            size: file_size,
            uploader_id: user.id,
            content_type: "text/plain",
            filename: "small.txt",
            error: nil,
            sha256: Digest::SHA256.hexdigest(file),
            media_file_id: a_kind_of(String)
          }.stringify_keys)
        end
      end

      context "when only first part of file has been sent" do
        before do
          part_1_request
          complete_request
        end

        it "responds with upload details with 'completed' state" do
          expect(without_timestamps(upload_get_request.body)).to eq({
            id: upload_id,
            md5: md5,
            state: "completed",
            media_store_id: "database",
            size: file_size,
            uploader_id: user.id,
            content_type: "text/plain",
            filename: "small.txt",
            media_file_id: nil,
            error: nil,
            sha256: nil,
          }.stringify_keys)
        end

        it "2nd call responds with upload details with 'failed' state "\
           "but without sha256 and media_file_id values and with error" do
          upload_get_request
          sleep 1 # ugly!
          response_body = upload_get_request.body
          expect(without_timestamps(response_body)).to match({
            id: upload_id,
            md5: md5,
            state: "failed",
            media_store_id: "database",
            size: file_size,
            uploader_id: user.id,
            content_type: "text/plain",
            filename: "small.txt",
            media_file_id: nil,
            sha256: nil,
            error: a_kind_of(String)
          }.stringify_keys)
        end
      end
    end
  end
end
